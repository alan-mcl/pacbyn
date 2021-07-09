
/*
PACBYN

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see https://www.gnu.org/licenses/.
*/

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
		ColouringKey colouringKeyMethod = ColouringKey.INT_SEQ;

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
			else if (args[i].equalsIgnoreCase("-?"))
			{
				printUsage();
				System.exit(1);
			}
			else
			{
				printUsage();
				throw new RuntimeException("Invalid arg: "+args[i]);
			}
		}

		if (imageName == null)
		{
			System.out.println("Argument -i [input image file] is is required");
			printUsage();
			System.exit(1);
		}
		if (outputName == null)
		{
			System.out.println("Argument -o [output image file] is is required");
			printUsage();
			System.exit(1);
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

		int[] palette = new int[palletColours];

//		palette = palletizeFloydSteinberg(palletColours, pixelsInRow, inputPixels);
//		palette = palletizeGimp(palletColours, pixelsInRow, inputPixels);
//		palette = palletizeSimple(palletColours, inputPixels);
		palette = palletizeKMeans(palletColours, inputPixels);

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
	private static void printUsage()
	{
		System.out.println("USAGE: java pacbyn.Main -i [input file] -o [output file] -n [pallette colours] -w [image size]");
		System.out.println();
		System.out.println("Required args:");
		System.out.println(" -i : input image file (requires a square image)");
		System.out.println(" -o : output image file");
		System.out.println();
		System.out.println("Optional args:");
		System.out.println(" -n : number of colours in the pallette (default 20)");
		System.out.println(" -w : output image size in pixels (default 40)");
		System.out.println(" -k : pallette key gen method, one of INT_SEQ, INT_RAND, ALPHA_SEQ, ALPHA_RAND (default INT_SEQ)");
		System.out.println(" -d : create a debug output image, with all cells coloured in");
		System.out.println(" -? : print this help message");
		System.out.println();
		System.out.println("Example:");
		System.out.println(" java pacbyn.Main -i input.png -o output.png -n 10 -w 55 -k ALPHA_SEQ");

	}

	/*-------------------------------------------------------------------------*/
	private static int[] palletizeGimp(
		int palletColours,
		int pixelsInRow,
		int[] inputPixels)
	{
		// source: https://www.qtcentre.org/threads/36385-Posterizes-an-image-with-results-identical-to-Gimp-s-Posterize-command

		int levels = palletColours / 2;
		levels--;
		double sr, sg, sb;
		int dr, dg, db;
		for (int i = 0; i < inputPixels.length; ++i)
		{
			int val = inputPixels[i];

			// to make sr, sg, sb between 0 and 1
			sr = ((val >> 16) & 0xFF) / 255.0;
			sg = ((val >> 8) & 0xFF) / 255.0;
			sb = ((val >> 0) & 0xFF) / 255.0;

			//rounding and NOT TRUNCATING
			dr = (int)(255 * Math.round(sr * levels) / levels);
			dg = (int)(255 * Math.round(sg * levels) / levels);
			db = (int)(255 * Math.round(sb * levels) / levels);

			inputPixels[i] = (inputPixels[i] & 0xFF000000) | dr<<16 | dg<<8 | db<<0;
		}

		Set<Integer> colours = new HashSet<>();

		for (int i = 0; i < inputPixels.length; i++)
		{
			colours.add(inputPixels[i]);
		}

		if (colours.size() > palletColours)
		{
			throw new RuntimeException("Error - too many colours in pallet: "+colours.size());
		}

		int[] result = new int[palletColours];
		List<Integer> list = new ArrayList<>(colours);
		for (int i = 0; i < result.length; i++)
		{
			result[i] = list.get(i);
		}

		return result;
	}

	/*-------------------------------------------------------------------------*/
		private static int[] palletizeKMeans(
			int palletColours,
			int[] inputPixels)
		{
			int[] pallet = KMeansPalette.palletize(palletColours, inputPixels);

			assignImagePixelsFromPallet(palletColours, inputPixels, pallet);

			return pallet;
		}

	/*-------------------------------------------------------------------------*/
	private static int[] palletizeSimple(
		int palletColours,
		int[] inputPixels)
	{
		Map<Integer, Integer> colourMap = new HashMap<>();

		// count all the colours
		for (int i = 0; i < inputPixels.length; ++i)
		{
			if (!colourMap.containsKey(inputPixels[i]))
			{
				colourMap.put(inputPixels[i], 0);
			}

			colourMap.put(inputPixels[i], colourMap.get(inputPixels[i])+1);
		}

		// get the top N out
		Set<Integer> pallet = new HashSet<>();

		for (int i=0; i<palletColours; i++)
		{
			int col = -1;
			int max = 0;
			for (Integer colour : colourMap.keySet())
			{
				if (colourMap.get(colour) > max)
				{
					max = colourMap.get(colour);
					col = colour;
				}
			}

			pallet.add(col);
			colourMap.remove(col);
		}

		int[] result = new int[palletColours];
		List<Integer> list = new ArrayList<>(pallet);
		for (int i = 0; i < result.length; i++)
		{
			result[i] = list.get(i);
		}

		assignImagePixelsFromPallet(palletColours, inputPixels, result);

		return result;
	}

	/*-------------------------------------------------------------------------*/
	private static void assignImagePixelsFromPallet(int palletColours,
		int[] inputPixels, int[] palletArray)
	{
		Set<Integer> pallet = new HashSet<>();

		for (int i = 0; i < palletArray.length; i++)
		{
			pallet.add(palletArray[i]);
		}

		// assign image pixels from the pallet
		int r1, g1, b1, r2, g2, b2;
		for (int i = 0; i < inputPixels.length; ++i)
		{
			int val = inputPixels[i];

			if (pallet.contains(val))
			{
				continue;
			}

			r1 = ((val >> 16) & 0xFF);
			g1 = ((val >> 8) & 0xFF);
			b1 = ((val >> 0) & 0xFF);

			int palletCol = 0;
			double minDist = Double.MAX_VALUE;

			for (Integer col : pallet)
			{
				r2 = ((col >> 16) & 0xFF);
				g2 = ((col >> 8) & 0xFF);
				b2 = ((col >> 0) & 0xFF);

				double dist = Math.sqrt(Math.pow(r2 - r1, 2) + Math.pow(g2 - g1, 2) + Math.pow(b2 - b1, 2));

				if (dist < minDist)
				{
					minDist = dist;
					palletCol = col;
				}
			}

			inputPixels[i] = palletCol;
		}

		Set<Integer> colours = new HashSet<>();

		for (int i = 0; i < inputPixels.length; i++)
		{
			colours.add(inputPixels[i]);
		}

		if (colours.size() > palletColours)
		{
			throw new RuntimeException("Error - too many colours in pallet: "+colours.size());
		}
	}

	/*-------------------------------------------------------------------------*/
	private static int[] palletizeFloydSteinberg(int palletColours,
		int pixelsInRow,
		int[] inputPixels)
	{
		int[] palette;
		int[] colorPalette = Quantize.createRandomColorPalette(256);
		palette = Quantize.quantize(
			inputPixels,
			pixelsInRow,
			pixelsInRow,
			colorPalette,
			palletColours,
			true,
			Quantize.ReductionStrategy.BETTER_CONTRAST);
		return palette;
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
				for (byte i = 0; i < palette.length; i++)
				{
					paletteKey.put(palette[i], new String(new char[]{(char)(i + 97)}));
				}
				break;

			case ALPHA_RAND:
				for (int i = 0; i < palette.length; i++)
				{
					String s;

					do
					{
						s = new String(new char[]{(char)(r.nextInt(26) + 97)});
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
