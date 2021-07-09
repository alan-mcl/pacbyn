package mclachlan.pacbyn;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Source: https://www.baeldung.com/java-k-means-clustering-algorithm
 */
public class KMeansPalette
{
	public static int[] palletize(int palletColours, int[] inputPixels)
	{
		List<Record> pixels = new ArrayList<>();

		for (int i = 0; i < inputPixels.length; i++)
		{
			int val = inputPixels[i];

			double r = ((val >> 16) & 0xFF);
			double g = ((val >> 8) & 0xFF);
			double b = ((val >> 0) & 0xFF);

			Map<String, Double> features = new HashMap<>();
			features.put("r", r);
			features.put("g", g);
			features.put("b", b);

			pixels.add(new Record("("+r+","+g+","+b+")", features));
		}

		Map<Centroid, List<Record>> clusters = KMeans.fit(pixels, palletColours, new EuclideanDistance(), 1000);

		/*clusters.forEach((key, value) -> {
			System.out.println("-------------------------- CLUSTER ----------------------------");

			// Sorting the coordinates to see the most significant tags first.
			System.out.println(key));
			String members = String.join(", ", value.stream().map(Record::getDescription).collect(toSet()));
			System.out.print(members);

			System.out.println();
			System.out.println();
		});*/

		int[] result = new int[palletColours];

		int n=0;
		for (Centroid key : clusters.keySet())
		{
			int r = key.getCoordinates().get("r").intValue();
			int g = key.getCoordinates().get("g").intValue();
			int b = key.getCoordinates().get("b").intValue();

			int col = ((r << 16) | (g << 8) | b) & 0xFFFFFF;

			result[n++] = col;
		};

		return result;
	}
}

class KMeans
{
	private static final Random random = new Random();

	public static Map<Centroid, List<Record>> fit(List<Record> records,
		int k,
		Distance distance,
		int maxIterations)
	{
		List<Centroid> centroids = randomCentroids(records, k);
		Map<Centroid, List<Record>> clusters = new HashMap<>();
		Map<Centroid, List<Record>> lastState = new HashMap<>();

		// iterate for a pre-defined number of times
		for (int i = 0; i < maxIterations; i++)
		{
			boolean isLastIteration = i == maxIterations - 1;

			// in each iteration we should find the nearest centroid for each record
			for (Record record : records)
			{
				Centroid centroid = nearestCentroid(record, centroids, distance);
				assignToCluster(clusters, record, centroid);
			}

			// if the assignments do not change, then the algorithm terminates
			boolean shouldTerminate = isLastIteration || clusters.equals(lastState);
			lastState = clusters;
			if (shouldTerminate)
			{
				break;
			}

			// at the end of each iteration we should relocate the centroids
			centroids = relocateCentroids(clusters);
			clusters = new HashMap<>();
		}

		return lastState;
	}

	private static List<Centroid> randomCentroids(List<Record> records, int k)
	{
		List<Centroid> centroids = new ArrayList<>();
		Map<String, Double> maxs = new HashMap<>();
		Map<String, Double> mins = new HashMap<>();

		for (Record record : records)
		{
			record.getFeatures().forEach((key, value) -> {
				// compares the value with the current max and choose the bigger value between them
				maxs.compute(key, (k1, max) -> max == null || value > max ? value : max);

				// compare the value with the current min and choose the smaller value between them
				mins.compute(key, (k1, min) -> min == null || value < min ? value : min);
			});
		}

		Set<String> attributes = records.stream()
			.flatMap(e -> e.getFeatures().keySet().stream())
			.collect(toSet());
		for (int i = 0; i < k; i++)
		{
			Map<String, Double> coordinates = new HashMap<>();
			for (String attribute : attributes)
			{
				double max = maxs.get(attribute);
				double min = mins.get(attribute);
				coordinates.put(attribute, random.nextDouble() * (max - min) + min);
			}

			centroids.add(new Centroid(coordinates));
		}

		return centroids;
	}

	private static Centroid nearestCentroid(Record record,
		List<Centroid> centroids, Distance distance)
	{
		double minimumDistance = Double.MAX_VALUE;
		Centroid nearest = null;

		for (Centroid centroid : centroids)
		{
			double currentDistance = distance.calculate(record.getFeatures(), centroid.getCoordinates());

			if (currentDistance < minimumDistance)
			{
				minimumDistance = currentDistance;
				nearest = centroid;
			}
		}

		return nearest;
	}

	private static void assignToCluster(Map<Centroid, List<Record>> clusters,
		Record record,
		Centroid centroid)
	{
		clusters.compute(centroid, (key, list) -> {
			if (list == null)
			{
				list = new ArrayList<>();
			}

			list.add(record);
			return list;
		});
	}

	private static Centroid average(Centroid centroid, List<Record> records)
	{
		if (records == null || records.isEmpty())
		{
			return centroid;
		}

		Map<String, Double> average = centroid.getCoordinates();
		records.stream().flatMap(e -> e.getFeatures().keySet().stream())
			.forEach(k -> average.put(k, 0.0));

		for (Record record : records)
		{
			record.getFeatures().forEach(
				(k, v) -> average.compute(k, (k1, currentValue) -> v + currentValue)
			);
		}

		average.forEach((k, v) -> average.put(k, v / records.size()));

		return new Centroid(average);
	}

	private static List<Centroid> relocateCentroids(
		Map<Centroid, List<Record>> clusters)
	{
		return clusters.entrySet().stream().map(e -> average(e.getKey(), e.getValue())).collect(toList());
	}
}

/*-------------------------------------------------------------------------*/
class Record
{
	private final String description;
	private final Map<String, Double> features;

	public Record(String description,
		Map<String, Double> features)
	{
		this.description = description;
		this.features = features;
	}

	public String getDescription()
	{
		return description;
	}

	public Map<String, Double> getFeatures()
	{
		return features;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Record))
		{
			return false;
		}

		Record record = (Record)o;

		if (!description.equals(record.description))
		{
			return false;
		}
		return features.equals(record.features);
	}

	@Override
	public int hashCode()
	{
		int result = description.hashCode();
		result = 31 * result + features.hashCode();
		return result;
	}
}

class Centroid
{

	private final Map<String, Double> coordinates;

	public Map<String, Double> getCoordinates()
	{
		return coordinates;
	}

	public Centroid(
		Map<String, Double> coordinates)
	{
		this.coordinates = coordinates;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (!(o instanceof Centroid))
		{
			return false;
		}

		Centroid centroid = (Centroid)o;

		return getCoordinates().equals(centroid.getCoordinates());
	}

	@Override
	public int hashCode()
	{
		return getCoordinates().hashCode();
	}

	@Override
	public String toString()
	{
		return "Centroid{" +
			"coordinates=" + coordinates +
			'}';
	}
}

interface Distance
{
	double calculate(Map<String, Double> f1, Map<String, Double> f2);
}

class EuclideanDistance implements Distance
{

	@Override
	public double calculate(Map<String, Double> f1, Map<String, Double> f2)
	{
		double sum = 0;
		for (String key : f1.keySet())
		{
			Double v1 = f1.get(key);
			Double v2 = f2.get(key);

			if (v1 != null && v2 != null)
			{
				sum += Math.pow(v1 - v2, 2);
			}
		}

		return Math.sqrt(sum);
	}
}
