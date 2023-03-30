import PMTiles.PMTiles;
import PMTiles.RangeResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class Main {

  public static void main(String[] args) {
    String url =
      "https://tmizu23.github.io/PMTiles-Java-ObjectiveC/sample.pmtiles";
    PMTiles pmtiles = new PMTiles(url, null, null);

    System.out.println("getHeader");
    pmtiles
      .getHeader()
      .thenAccept(header -> {
        System.out.println("Header: " + header.getMaxZoom());
        System.out.println("Header: " + header.getMinZoom());
        System.out.println("Header: " + header.getCenterLat());
        System.out.println("Header: " + header.getCenterLon());
      })
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });

    System.out.println("getZxy, 19, 467278,201856");
    pmtiles
      .getZxy(19, 467278, 201856)
      .thenAccept(result -> {
        if (result.isPresent()) {
          RangeResponse rangeResponse = result.get();
          byte[] imageData = rangeResponse.data.array();
          InputStream in = new ByteArrayInputStream(imageData);
          try {
            BufferedImage image = ImageIO.read(in);
            SwingUtilities.invokeLater(() -> {
              JFrame frame = new JFrame("Tile Image");
              frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
              frame.add(new JLabel(new ImageIcon(image)));
              frame.pack();
              frame.setVisible(true);
            });
          } catch (IOException e) {
            e.printStackTrace();
          }
        } else {
          System.out.println("No image data available.");
        }
      });
  }
}
