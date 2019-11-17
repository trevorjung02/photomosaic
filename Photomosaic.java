import java.awt.image.AffineTransformOp;
import java.awt.geom.AffineTransform;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.util.*;
import java.nio.file.*;
import java.util.stream.Collectors;

class Photomosaic {
	public static void main(String[] args) throws IOException {
		Scanner sc = new Scanner(System.in);
		System.out.println("Type the path of the image");
		System.out.println("Example: C:\\Users\\Me\\Photos\\ImageName.jpg");
		String imagePath = sc.nextLine();
		BufferedImage myPicture = ImageIO.read(new File(imagePath));
		System.out.println("\nType the folder including the images to be used in the mosaic");
		System.out.println("Example: C:\\Users\\Me\\MosaicResources");
		String resourcesPath = sc.nextLine();
		System.out.println("\nType the width in pixels of the new image");
		System.out.println("Example: 1920");
		double scaleFactor = sc.nextInt()/(double)myPicture.getWidth(); 
		sc.nextLine();
		System.out.println("\nType the path of the final image (including image type)");
		System.out.println("Example: C:\\Users\\Me\\Photos\\NewImageName.jpg");
		String outputPath = sc.nextLine();
		sc.close();
		
		final File resourcesDir = new File(resourcesPath);
		
		BufferedImage resized = new BufferedImage((int)(myPicture.getWidth()*scaleFactor), 
												  (int)(myPicture.getHeight()*scaleFactor), myPicture.getType());
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(myPicture, 0, 0, resized.getWidth(), resized.getHeight(), 
							   0, 0, myPicture.getWidth(), myPicture.getHeight(), null);

		int pixelFactor = 110;
		double pixelSize = (double) Math.min(resized.getHeight(), resized.getWidth()) / pixelFactor;

		HashMap<Color, List<ImageAverage>> images = processImages(resourcesDir, (int) pixelSize);
		for(double a = 0; a < (resized.getHeight() / pixelSize); a++) {
			for(double b = 0; b < (resized.getWidth() / pixelSize); b++) {
				Color average = averageColor(resized, (int)(Math.ceil(b*pixelSize)), (int)(Math.ceil(a*pixelSize)), 
											Math.min((int)(Math.ceil((b+1)*pixelSize)), resized.getWidth()),
											Math.min((int)(Math.ceil((a+1)*pixelSize)), resized.getHeight()));
				g.drawImage(closestImage(average, images),
					(int)(Math.ceil(b*pixelSize)), (int)(Math.ceil(a*pixelSize)), 
					Math.min((int)(Math.ceil((b+1)*pixelSize)), resized.getWidth()), 
					Math.min((int)(Math.ceil((a+1)*pixelSize)), resized.getHeight()), 
					0, 0, (int) pixelSize, (int) pixelSize, null);
			}
		}
		ImageIO.write(resized, "jpg", new File(outputPath)); 
		System.out.println("Your image is complete");
		g.dispose();
	}

	public static BufferedImage closestImage(Color c, HashMap<Color, List<ImageAverage>> images) {
		double minDistance = 257;
		ImageAverage min = null;
		Color keyColor = colorKey(c);
		if(images.containsKey(keyColor)) {
			for(ImageAverage curImage : images.get(keyColor)) {
				double curDistance = colorDistance(c, curImage.average);
				if(curDistance < minDistance) {
					minDistance = curDistance;
					min = curImage; 
				}
			}
		}
		else {
			for(List<ImageAverage> i : images.values()) {
				for(ImageAverage curImage : i) {
					double curDistance = colorDistance(c, curImage.average);
					if(curDistance < minDistance) {
						minDistance = curDistance;
						min = curImage;
					}
				}
			}
			images.put(keyColor, new ArrayList<ImageAverage>());
			images.get(keyColor).add(min);
		}
		return min.image;
	}

	public static Color colorKey(Color c) {
		int roundTo = 25;
		return new Color(roundTo*Math.round(c.getRed()/roundTo), roundTo*Math.round(c.getGreen()/roundTo), roundTo*Math.round(c.getBlue()/roundTo));
	}

	public static double colorDistance(Color c1, Color c2) {
		int c1Red = c1.getRed();
		int c1Green = c1.getGreen();
		int c1Blue = c1.getBlue();
		int c2Red = c2.getRed();
		int c2Green = c2.getGreen();
		int c2Blue = c2.getBlue();
		return Math.sqrt(Math.pow(c1Red-c2Red, 2) + Math.pow(c1Green-c2Green, 2) + Math.pow(c1Blue-c2Blue, 2));
	}

	public static Color averageColor(BufferedImage image, int x1, int y1, int x2, int y2) {
		int totalRed = 0;
		int totalGreen = 0;
		int totalBlue = 0;
		int sectionArea = 0;
		for(int i = y1; i < y2; i++) {
			for(int j = x1; j < x2; j++) {
				sectionArea++;
				int rgb = image.getRGB(j, i);
				totalRed += (rgb>>16)&0xff;
				totalGreen += (rgb>>8)&0xff;
				totalBlue += (rgb)&0xff;
			}
		}
		return new Color(Math.min(255,(int)(totalRed/sectionArea)), Math.min(255, (int)(totalGreen/sectionArea)), 
											   Math.min(255, (int)(totalBlue/sectionArea)));
	}

	public static BufferedImage resizeImage(BufferedImage image, int length) {
		BufferedImage resized = new BufferedImage(length, length, image.getType());
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, resized.getWidth(), resized.getHeight(), 
							   0, 0, image.getWidth(), image.getHeight(), null);
		g.dispose();
		return resized;
	}

	public static HashMap<Color, List<ImageAverage>> processImages(final File folder, int size) throws IOException {
		HashMap<Color, List<ImageAverage>> map = new HashMap<>();
		for(final File fileEntry : folder.listFiles()) {
			String fileName = fileEntry.getName();
			BufferedImage readImage = ImageIO.read(fileEntry);
			if(readImage == null) {
				continue;
			}
			BufferedImage image = resizeImage(readImage, size);
			Color average = averageColor(image, 0, 0, image.getWidth(), image.getHeight());
			Color keyColor = colorKey(average);
			if(!map.containsKey(keyColor)) {
				map.put(keyColor, new ArrayList<ImageAverage>());
			} 
			map.get(keyColor).add(new ImageAverage(image, average));
		}
       	return map;
	}

	static class ImageAverage {
		BufferedImage image;
		Color average;

		public ImageAverage(BufferedImage image, Color average) {
			this.image = image;
			this.average = average;
		}
	}
}