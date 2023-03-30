package PMTiles;

public class EtagMismatch extends RuntimeException {

  public EtagMismatch() {
    super();
  }

  public EtagMismatch(String message) {
    super(message);
  }
}
