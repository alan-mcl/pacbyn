package mclachlan.pacbyn;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.util.*;
import javax.imageio.ImageIO;

public class Main
{
	enum ColouringKey
	{
		INT_SEQ, INT_RAND, ALPHA_SEQ, ALPHA_RAND
	}

	public static void main(String[] args) throws Exception
	{
		int palletColours = 20;
		int pixelsInRow = 40;
		String imageName = null;
		String outputName = "output.png";
		boolean debug = false;
		ColouringKey colouringKeyMethod = ColouringKey.INT_RAND;

		for (int i = 0; i < args.length; i++)
		{
			if (args[i].equalsIgnoreCase("-d"))
			{
				debug = true;
			}
			else if (args[i].equalsIgnoreCase("-i"))
			{
				imageName = args[++i];
			}
			else if (args[i].equalsIgnoreCase("-o"))
			{
				outputName = args[++i];
			}
			else if (args[i].equalsIgnoreCase("-n"))
			{
				palletColours = Integer.parseInt(args[++i]);
			}
			else if (args[i].equalsIgnoreCase("-w"))
			{
				pixelsInRow = Integer.parseInt(args[++i]);
			}
			else if (args[i].equalsIgnoreCase("-k"))
			{
				colouringKeyMethod = ColouringKey.valueOf(args[++i]);
			}
		}


		BufferedImage image = ImageIO.read(new File(imageName));
		int imageHeight = image.getHeight();
		int imageWidth = image.getWidth();

		// validate square image
		if (imageHeight != imageWidth)
		{
			throw new Exception("requires a square image");
		}

		// lose resolution by resizing
		Image resizedImage;
		if (pixelsInRow < imageWidth)
		{
			resizedImage = image.getScaledInstance(pixelsInRow, pixelsInRow, Image.SCALE_SMOOTH);
		}
		else
		{
			resizedImage = image;
		}

		int[] inputPixels = grabPixels(resizedImage, pixelsInRow, pixelsInRow);

		int[] colorPalette = Quantize.createRandomColorPalette(256);
		int[] palette = Quantize.quantize(
			inputPixels,
			pixelsInRow,
			pixelsInRow,
			colorPalette,
			palletColours,
			true,
			Quantize.ReductionStrategy.BETTER_CONTRAST);

		Map<Integer, String> paletteKey = generateColouringKey(palette, colouringKeyMethod);

		// A4 size
		int outputWidth = 2480;
		int outputHeight = 3508;
		BufferedImage displayImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);

		Graphics g = displayImage.getGraphics();
		g.setFont(new Font("Arial Black", Font.PLAIN, 25));

		// where we draw the grid
		int startx = 150;
		int starty = 150;
		int gridWidth = 2180;
		int gridHeight = 2180;

		int cellWidth = gridWidth / pixelsInRow;
		int cellHeight = cellWidth;

		// fill the output image with white
		for (int i = 0; i < outputWidth; i++)
		{
			for (int j = 0; j < outputHeight; j++)
			{
				displayImage.setRGB(i, j, 0xFFFFFFFF);
			}
		}

		// draw the grid
		for (int i = 0; i < pixelsInRow; i++)
		{
			for (int j = 0; j < pixelsInRow; j++)
			{
				int x = startx + (i * cellWidth);
				int y = starty + (j * cellHeight);

				g.setColor(Color.DARK_GRAY);
				g.drawRect(x, y, cellWidth, cellHeight);

				int cellColour = inputPixels[j * pixelsInRow + i];
				g.setColor(Color.GRAY);
				g.drawString(paletteKey.get(cellColour), x + cellWidth / 4, y + cellHeight / 4 * 3);

				if (debug)
				{
					for (int k = 0; k < cellWidth; k++)
					{
						g.setColor(new Color(cellColour));
						g.fillRect(x, y, cellWidth, cellHeight);
					}
				}
			}
		}

		// draw the key
		int keyStartX = 100;
		int keyStartY = 2500;

		cellWidth = cellWidth * 2;
		cellHeight = cellHeight * 2;

		for (int i = 0; i < palletColours; i++)
		{
			int cellColour = palette[i];

			int x = keyStartX + (i * cellWidth);
			int y = keyStartY;

			g.setColor(new Color(cellColour));
			g.fillRect(x + 2, y + 2, cellWidth - 4, cellHeight - 4);

			g.setColor(Color.BLACK);
			g.drawString(paletteKey.get(palette[i]), x + cellWidth / 4, y + cellHeight / 4 * 3);
		}

		// ----

		ImageIO.write(displayImage, "png", new File(outputName));
	}

	/*-------------------------------------------------------------------------*/
	private static Map<Integer, String> generateColouringKey(
		int[] palette,
		ColouringKey method)
	{
		Random r = new Random(System.currentTimeMillis());

		Map<Integer, String> paletteKey = new HashMap<>();

		switch (method)
		{
			case INT_SEQ:
				for (int i = 0; i < palette.length; i++)
				{
					paletteKey.put(palette[i], String.valueOf(i));
				}
				break;

			case INT_RAND:
				for (int i = 0; i < palette.length; i++)
				{
					String s;

					do
					{
						s = String.valueOf(r.nextInt(99));
					}
					while (paletteKey.containsValue(s));

					paletteKey.put(palette[i], s);
				}
				break;

			case ALPHA_SEQ:
				for (byte i=0; i<palette.length; i++)
				{
					paletteKey.put(palette[i], new String(new char[]{(char)(i+97)}));
				}
				break;

			case ALPHA_RAND:
				for (int i = 0; i < palette.length; i++)
				{
					String s;

					do
					{
						s = new String(new char[]{(char)(r.nextInt(26)+97)});
					}
					while (paletteKey.containsValue(s));

					paletteKey.put(palette[i], s);
				}

				break;
		}

		return paletteKey;
	}


	/*-------------------------------------------------------------------------*/
	private static int[] grabPixels(Image image, int width,
		int height) throws InterruptedException
	{
		int[] result = new int[width * height];
		PixelGrabber grabber = new PixelGrabber(
			image, 0, 0, width, height, result, 0, width);

		grabber.grabPixels();

		return result;
	}
}
