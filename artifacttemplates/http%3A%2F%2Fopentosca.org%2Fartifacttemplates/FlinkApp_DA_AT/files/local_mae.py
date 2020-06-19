from flink.plan.Environment import get_environment
from flink.plan.Constants import WriteMode
from flink.functions.MapFunction import MapFunction
from flink.functions.JoinFunction import JoinFunction
from sklearn import cross_validation, linear_model
import numpy as np
import random


def add_noise(data, seed=1, sigma=0.3):
    random.seed(seed)
    return [random.gauss(x, sigma) for x in data]


def fit_estimator(data):
  """
  Train an estimator on the data and return it
  data : DataProvider
  """
  X_raw = data.X_train[:,1]
  X = np.array(add_noise(X_raw)).reshape(-1, 1)
  y = data.y_train[:,1]
  est = linear_model.LinearRegression()
  est.fit(X, y)
  return est


class AnalyticalFunction(object):
  def __init__(self, a=5, b=2):
    self.a = a
    self.b = b
   
  def value(self, x):
    return self.a * x + self.b


class DataProvider:
  """
  Provide access to our training and test sets
  """
  def __init__(self, size=100, test_size=0.1):
    fn = AnalyticalFunction()
    X_raw = range(50,size+50)
    X_data = np.array(X_raw).reshape(-1, 1)
    y_data = np.array([fn.value(x) for x in X_raw])

    # we need to add an index column
    # it enables us to later join predictions and actual values
    index = np.array(range(size)).reshape(-1, 1)
    self.X = np.c_[index, X_data]
    self.y = np.c_[index, y_data]

    # split up the data set in train and test sets
    self.X_train, self.X_test, self.y_train, self.y_test = cross_validation.train_test_split(self.X, self.y, test_size=test_size, random_state=1)


class AbsoluteErrorJoin(JoinFunction):
  """
  Rich function that calculates the absolute error on join
  """
  def join(self, a, b):
    return abs(a[1] - b[1])


class MapEstimator(MapFunction):
  """
  Rich function, needs to be initialised with an estimator
  """
  def __init__(self, est):
    super(MapEstimator, self).__init__()
    self.est = est
  
  def map(self, row):
    i, x = row
    x = np.array(x).reshape(1, -1)
    return (i, self.est.predict(x).tolist()[0])


if __name__ == "__main__":
  env = get_environment()
  env.set_parallelism(3)

  # create our data set
  prov = DataProvider(size=10000)
  # fit an estimator
  est = fit_estimator(prov)

  # get the X_test set as list and unpack it
  # then map our Estimator to get predictions
  data_pred = env.from_elements(*prov.X_test.tolist()) \
            .map(MapEstimator(est)) \

  # get the actual values for the test set
  data_actual = env.from_elements(*prov.y_test.tolist())

  # join predictions and actual results on index column
  # calculate the mean absolute error by
  #   1. calculating absolute error while joining
  #   2. adding all errors in parallel
  #   3. adding the reduce results again without parallelism
  #   4. dividing by the test set size
  result = data_pred.join(data_actual).where(0).equal_to(0).using(AbsoluteErrorJoin()) \
               .reduce(lambda a, b: a + b) \
               .reduce(lambda a, b: a + b).set_parallelism(1) \
               .map(lambda x: x / len(prov.y_test.tolist()))

  # write the result without parallelism
  result.write_text('result.txt', write_mode=WriteMode.OVERWRITE) \
        .set_parallelism(1)

  env.execute(local=True)
