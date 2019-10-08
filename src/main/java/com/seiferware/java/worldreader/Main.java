package com.seiferware.java.worldreader;

import com.google.protobuf.CodedInputStream;
import org.omg.CORBA.DoubleHolder;
import org.omg.CORBA.IntHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Main {
	private static void createImage(String path, WorldOuterClass.World.DoubleMatrixOrBuilder data) throws IOException {
		createImage(path, data, ColorCalculator.DEFAULT);
	}
	private static void createImage(String path, WorldOuterClass.World.DoubleMatrixOrBuilder data, ColorCalculator calc) throws IOException {
		DoubleHolder min = new DoubleHolder(Double.MAX_VALUE);
		DoubleHolder max = new DoubleHolder(Double.MIN_VALUE);
		data.getRowsList().stream().flatMap(d -> d.getCellsList().stream()).forEach(d -> {
			if(min.value > d) {
				min.value = d;
			}
			if(max.value < d) {
				max.value = d;
			}
		});
		BufferedImage bim = new BufferedImage(data.getRows(0).getCellsCount(), data.getRowsCount(), BufferedImage.TYPE_INT_RGB);
		for(int y = 0; y < data.getRowsCount(); y++) {
			WorldOuterClass.World.DoubleRow row = data.getRows(y);
			for(int x = 0; x < row.getCellsCount(); x++) {
				int color = calc.getColorForValue(row.getCells(x), min.value, max.value);
				bim.setRGB(x, y, color);
			}
		}
		ImageIO.write(bim, "png", new File(path));
	}
	private static void getDistribution(WorldOuterClass.World.DoubleMatrixOrBuilder data, int precision) {
		DoubleHolder min = new DoubleHolder(Double.MAX_VALUE);
		DoubleHolder max = new DoubleHolder(Double.MIN_VALUE);
		IntHolder minInt = new IntHolder();
		IntHolder maxInt = new IntHolder();
		Map<Integer, IntHolder> values = new HashMap<>();
		data.getRowsList().stream().flatMap(d -> d.getCellsList().stream()).forEach(d -> {
			int intVal = Math.toIntExact(Math.round(d * precision));
			if(min.value > d) {
				min.value = d;
				minInt.value = intVal;
			}
			if(max.value < d) {
				max.value = d;
				maxInt.value = intVal;
			}
			if(!values.containsKey(intVal)) {
				values.put(intVal, new IntHolder(0));
			}
			values.get(intVal).value++;
		});
		IntHolder zero = new IntHolder(0);
		for(int i = minInt.value; i <= maxInt.value; i++) {
			System.out.println(i / (float) precision + "\t" + values.getOrDefault(i, zero).value);
		}
	}
	public static void main(String[] args) {
		if(args.length == 1) {
			File f = new File(args[0]);
			if(f.exists()) {
				if(f.isDirectory()) {
					processDirectory(f);
				} else {
					processFile(f.getAbsolutePath());
				}
			} else {
				System.out.println("Could not locate specified file or directory.");
			}
		} else if(args.length == 0) {
			processDirectory(new File("."));
		} else {
			System.out.println("The only valid command line argument is either a world file, or a directory with world files. If no argument is specified, the current working directory will be used.");
		}
	}
	private static void processDirectory(File dir) {
		File[] files = dir.listFiles((dir1, name) -> name.endsWith(".world"));
		if(files != null && files.length > 0) {
			Stream.of(files).forEach(file -> {
				System.out.println("Processing " + file.getName());
				processFile(file.getAbsolutePath());
			});
		} else {
			System.out.println("Did not find any *.world files in specified directory.");
		}
	}
	private static void processFile(String fileName) {
		processFile(fileName, fileName.endsWith(".world") ? fileName.substring(0, fileName.length() - 6) : fileName);
	}
	private static void processFile(String fileName, String baseFileName) {
		boolean needed = false;
		// Just because loading large world files is very slow.
		for(String nm : new String[]{"height", "temp", "rain", "rivers", "lakes"}) {
			if(!new File(baseFileName + "-" + nm + ".png").exists()) {
				needed = true;
				break;
			}
		}
		if(!needed) {
			return;
		}
		WorldOuterClass.World world;
		try (FileInputStream fis = new FileInputStream(new File(fileName))) {
			CodedInputStream cis = CodedInputStream.newInstance(fis);
			cis.setSizeLimit(Integer.MAX_VALUE);
			world = WorldOuterClass.World.parseFrom(cis);
		} catch (IOException e) {
			System.err.println("An error occurred: " + e.getMessage());
			return;
		}
		//getDistribution(world.getPrecipitationData(), 10);
		try {
			if(!new File(baseFileName + "-height.png").exists()) {
				createImage(baseFileName + "-height.png", world.getHeightMapData());
			}
			if(!new File(baseFileName + "-rain.png").exists()) {
				createImage(baseFileName + "-rain.png", world.getPrecipitationData());
			}
			if(!new File(baseFileName + "-temp.png").exists()) {
				createImage(baseFileName + "-temp.png", world.getTemperatureData());
			}
			if(!new File(baseFileName + "-rivers.png").exists()) {
				createImage(baseFileName + "-rivers.png", world.getRivermap(), (value, minValue, maxValue) -> {
					if(value == 0) {
						return 0;
					}
					return ColorCalculator.DEFAULT.getColorForValue(value, minValue, maxValue) | 255;
				});
			}
			if(!new File(baseFileName + "-lakes.png").exists()) {
				createImage(baseFileName + "-lakes.png", world.getLakemap(), (value, minValue, maxValue) -> {
					if(value == 0) {
						return 0;
					}
					return ColorCalculator.DEFAULT.getColorForValue(value, minValue, maxValue) | 255;
				});
			}
		} catch (IOException e) {
			System.err.println("An error occurred: " + e.getMessage());
		}
	}
	private interface ColorCalculator {
		ColorCalculator DEFAULT = (value, minValue, maxValue) -> {
			int result = (int) Math.max(0, Math.min(255, Math.round((value - minValue) * 255 / (maxValue - minValue))));
			return (result << 16) | (result << 8) | (result);
		};
		int getColorForValue(double value, double minValue, double maxValue);
	}
}
