import pennylane as qml
import numpy as np

def algo(x, y, z):
	qml.RZ(z, wires=[0])
	qml.RY(y, wires=[0])
	qml.RX(x, wires=[0])
	qml.CNOT(wires=[0, 1])
	return qml.expval(qml.PauliZ(wires=1))

def run_algo(device, args):
	print(args)
	circuit = qml.QNode(algo, device)
	result = circuit(float(args['X']), float(args['Y']), float(args['Z']))
	return result