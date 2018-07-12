package org.opentosca;

public class ConnectionDetail {

  private String hostname;

  private String port;

  private String name;

  private String user;

  private String password;

  public ConnectionDetail(String hostname, String port, String name, String user,
      String password) {
    this.hostname = hostname;
    this.port = port;
    this.name = name;
    this.user = user;
    this.password = password;
  }

  @Override
  public String toString() {
    return "jdbc:mysql://" + hostname
        + ":" + port + "/" + name
        + "; user='" + user + '\''
        + ", password='" + password + '\'';
  }
}
