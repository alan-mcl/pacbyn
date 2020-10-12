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
	static boolean debug = false;

	public static void main(String[] args) throws Exception
	{
		Random r = new Random(System.currentTimeMillis());

		int palletColours = 20;
		int pixelsInRow = 40;
		String imageName = null;
		String outputName = "output.png";

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
		}


		BufferedImage image = ImageIO.read(new File(imageName));
		int imageHeight = image.getHeight();
		int imageWidth = image.getWidth();

		// todo
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

		Map<Integer, String> paletteKey = new HashMap<>();
		for (int i = 0; i < palette.length; i++)
		{
			int key;

			do
			{
				key = r.nextInt(99);
			}
			while (paletteKey.containsKey(key));

			paletteKey.put(palette[i], ""+key);
		}

		// A4 size
		int outputWidth = 2480;
		int outputHeight = 3508;
		BufferedImage displayImage = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);

		Graphics graphics = displayImage.getGraphics();
		graphics.setFont(new Font("Arial Black", Font.PLAIN, 25));

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

		// test
//		for (int i = 0; i < pixelsInRow; i++)
//		{
//			for (int j = 0; j < pixelsInRow; j++)
//			{
//				displayImage.setRGB(i, j, inputPixels[i * pixelsInRow + j]);
//			}
//		}

		// draw the grid
		for (int i = 0; i < pixelsInRow; i++)
		{
			for (int j = 0; j < pixelsInRow; j++)
			{
				for (int k = 0; k < cellWidth; k++)
				{
					int x = startx + (i * cellWidth) + k;
					int y = starty + (j * cellHeight);
					displayImage.setRGB(x, y, 0x000000);

					x = startx + (i * cellWidth);
					y = starty + (j * cellHeight) + k;
					displayImage.setRGB(x, y, 0x000000);
				}

				int cellColour = inputPixels[j * pixelsInRow + i];
				int x = startx + (i * cellWidth);
				int y = starty + (j * cellHeight);
				graphics.setColor(Color.GRAY);
				graphics.drawString(paletteKey.get(cellColour), x + cellWidth/4, y + cellHeight/4*3);

				if (debug)
				{
					for (int k = 0; k < cellWidth; k++)
					{
						for (int m = 0; m < cellWidth; m++)
						{
							x = startx + (i * cellWidth) + k;
							y = starty + (j * cellHeight) + m;
							displayImage.setRGB(x, y, cellColour);
						}
					}
				}
			}
		}

		// draw the key

		int keyStartX = 200;
		int keyStartY = 2500;

		cellWidth = cellWidth * 2;
		cellHeight = cellHeight * 2;

		for (int i=0; i<palletColours; i++)
		{
			int cellColour = palette[i];

			for (int k = 2; k < cellWidth-2; k++)
			{
				for (int m = 2; m < cellWidth-2; m++)
				{
					int x = keyStartX + (i * cellWidth) + k;
					int y = keyStartY + m;
					displayImage.setRGB(x, y, cellColour);
				}
			}

			int x = keyStartX + (i * cellWidth);
			int y = keyStartY;
			graphics.setColor(Color.BLACK);
			graphics.drawString(paletteKey.get(palette[i]), x + cellWidth/4, y + cellHeight/4*3);
		}


		// ----

		ImageIO.write(displayImage, "png", new File(outputName));
	}


	private static int[] grabPixels(Image image, int width,
		int height) throws InterruptedException
	{
		int[] result = new int[width * height];
		PixelGrabber grabber = new PixelGrabber(
			image, 0, 0, width, height, result, 0, width);

		grabber.grabPixels();

		return result;
	}

	/*-------------------------------------------------------------------------*/
	public static byte getAlpha(int pixel)
	{
		return (byte)((pixel >> 24) & 0xFF);
	}

	/*-------------------------------------------------------------------------*/
	public static byte getRed(int pixel)
	{
		return (byte)((pixel >> 16) & 0xFF);
	}

	/*-------------------------------------------------------------------------*/
	public static byte getGreen(int pixel)
	{
		return (byte)((pixel >> 8) & 0xFF);
	}

	/*-------------------------------------------------------------------------*/
	public static byte getBlue(int pixel)
	{
		return (byte)(pixel & 0xFF);
	}

}
