/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.lib.algorithms;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.lib.algorithms.IntensityFeaturesPlugin.BasicFeatureComputer.Feature;
import qupath.lib.analysis.algorithms.FloatArraySimpleImage;
import qupath.lib.analysis.algorithms.SimpleImage;
import qupath.lib.analysis.features.CoocurranceMatrices;
import qupath.lib.analysis.features.HaralickFeatureComputer;
import qupath.lib.analysis.features.HaralickFeatures;
import qupath.lib.analysis.stats.RunningStatistics;
import qupath.lib.analysis.stats.StatisticsHelper;
import qupath.lib.color.ColorDeconvolutionStains;
import qupath.lib.color.ColorTransformer;
import qupath.lib.color.ColorTransformer.ColorTransformMethod;
import qupath.lib.common.GeneralTools;
import qupath.lib.geom.ImmutableDimension;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.stores.ImageRegionStore;
import qupath.lib.images.stores.TileListener;
import qupath.lib.images.tools.BufferedImageTools;
import qupath.lib.measurements.MeasurementList;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathDetectionObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.TMACoreObject;
import qupath.lib.plugins.AbstractInteractivePlugin;
import qupath.lib.plugins.PluginRunner;
import qupath.lib.plugins.parameters.ParameterList;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;
import qupath.lib.roi.interfaces.ROI;

/**
 * Plugin for calculating intensity-based features, including Haralick textures, within or around detections or tiles.
 * 
 * The ROIs of the detections can be used directly as masks, or else the textures can alternatively be 
 * calculated within square or circular regions around the object centroids.
 * This latter option makes it possible to calculate a high density of tiles (for example), and then to 
 * compute textures at different resolutions independently of the tile size.
 * 
 * TODO: Read entire region (where suitable) & tile that (if it makes sense...? may not scale up to whole slide images though...)
 * TODO: Improve use of static/non-static methods
 * 
 * @author Pete Bankhead
 *
 */
public class IntensityFeaturesPlugin extends AbstractInteractivePlugin<BufferedImage> {
	
	private final static Logger logger = LoggerFactory.getLogger(IntensityFeaturesPlugin.class);
	
	private ParameterList params;
	private boolean parametersInitialized = false;
	
	transient private ImageRegionStore<BufferedImage> regionStore;
	
	
	static enum RegionType {
		ROI, SQUARE, CIRCLE;
		
		public String toString() {
			switch(this) {
			case CIRCLE:
				return "Circular tiles";
			case ROI:
				return "ROI";
			case SQUARE:
				return "Square tiles";
			default:
				return "Unknown";
			}
		}
		
	}
	
	
	static enum FeatureColorTransform {
		
		OD("colorOD", "Optical density sum"),
		STAIN_1("colorStain1", "Color Deconvolution Stain 1"),
		STAIN_2("colorStain2", "Color Deconvolution Stain 2"),
		STAIN_3("colorStain3", "Color Deconvolution Stain 3"),
		RED("colorRed", "Red"),
		GREEN("colorGreen", "Green"),
		BLUE("colorBlue", "Blue"),
		HUE("colorHue", "Hue (mean only)"),
		SATURATION("colorSaturation", "Saturation"),
		BRIGHTNESS("colorBrightness", "Brightness"),
		
		CHANNEL_1("channel1", "Channel 1"),
		CHANNEL_2("channel2", "Channel 2"),
		CHANNEL_3("channel3", "Channel 3"),
		CHANNEL_4("channel4", "Channel 4"),
		CHANNEL_5("channel5", "Channel 5"),
		CHANNEL_6("channel6", "Channel 6"),
		CHANNEL_7("channel7", "Channel 7"),
		CHANNEL_8("channel8", "Channel 8"),
		
		;
		
		private String key;
		private String prompt;
		
		FeatureColorTransform(final String key, final String prompt) {
			this.key = key;
			this.prompt = prompt;
		}

		public String getPrompt(final ImageData<?> imageData) {
			ColorDeconvolutionStains stains = imageData == null ? null : imageData.getColorDeconvolutionStains();
			if (stains != null) {
				switch (this) {
				case STAIN_1:
					return stains.getStain(1).getName() + " (color deconvolved)";
				case STAIN_2:
					return stains.getStain(2).getName() + " (color deconvolved)";
				case STAIN_3:
					return stains.getStain(3).getName() + " (color deconvolved)";
				default:
					break;
				}
			}
			return prompt;
		}

		public String getKey() {
			return key;
		}

		public double[] getHaralickMinMax() {
			switch (this) {
			case HUE:
				return null;
			case RED:
			case GREEN:
			case BLUE:
			case BRIGHTNESS:
				return new double[]{0, 1};
			case SATURATION:
				return new double[]{0, 1};
			case OD:
				return new double[]{0, 2.5};
			case STAIN_1:
			case STAIN_2:
			case STAIN_3:
				return new double[]{0, 1.5};
			default:
				return null;
			}
		}
		
		
		public String getName(final ColorDeconvolutionStains stains) {
			switch (this) {
			case STAIN_1:
				return stains.getStain(1).getName();
			case STAIN_2:
				return stains.getStain(2).getName();
			case STAIN_3:
				return stains.getStain(3).getName();
			case HUE:
				return "Hue";
			case OD:
				return "OD Sum";
			default:
				return getPrompt(null);
			}
		}
		

		public boolean supportsImage(final ImageData<?> imageData) {
			switch (this) {
			case BRIGHTNESS:
			case HUE:
			case RED:
			case GREEN:
			case BLUE:
			case SATURATION:
				return imageData.getServer().isRGB();
			case OD:
			case STAIN_1:
			case STAIN_2:
			case STAIN_3:
				return imageData.isBrightfield() && imageData.getServer().isRGB();
			case CHANNEL_1:
				return !imageData.getServer().isRGB() && imageData.getServer().nChannels() >= 1;
			case CHANNEL_2:
				return !imageData.getServer().isRGB() && imageData.getServer().nChannels() >= 2;
			case CHANNEL_3:
				return !imageData.getServer().isRGB() && imageData.getServer().nChannels() >= 3;
			case CHANNEL_4:
				return !imageData.getServer().isRGB() && imageData.getServer().nChannels() >= 4;
			case CHANNEL_5:
				return !imageData.getServer().isRGB() && imageData.getServer().nChannels() >= 5;
			case CHANNEL_6:
				return !imageData.getServer().isRGB() && imageData.getServer().nChannels() >= 6;
			case CHANNEL_7:
				return !imageData.getServer().isRGB() && imageData.getServer().nChannels() >= 7;
			case CHANNEL_8:
				return !imageData.getServer().isRGB() && imageData.getServer().nChannels() >= 8;
			default:
				return false;
			}
		}
		
		public float[] getTransformedPixels(final BufferedImage img, int[] buf, final ColorDeconvolutionStains stains, float[] pixels) {
			if (pixels == null)
				pixels = new float[img.getWidth() * img.getHeight()];
			
			switch (this) {
			case BRIGHTNESS:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Brightness, pixels, stains);
			case HUE:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Hue, pixels, stains);
			case OD:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Optical_density_sum, pixels, stains);
			case RED:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Red, pixels, stains);
			case GREEN:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Green, pixels, stains);
			case BLUE:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Blue, pixels, stains);
			case SATURATION:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Saturation, pixels, stains);
			case STAIN_1:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Stain_1, pixels, stains);
			case STAIN_2:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Stain_2, pixels, stains);
			case STAIN_3:
				return ColorTransformer.getTransformedPixels(buf, ColorTransformMethod.Stain_3, pixels, stains);
				
			case CHANNEL_1:
				return img.getRaster().getSamples(0, 0, img.getWidth(), img.getHeight(), 0, pixels);
			case CHANNEL_2:
				return img.getRaster().getSamples(0, 0, img.getWidth(), img.getHeight(), 1, pixels);
			case CHANNEL_3:
				return img.getRaster().getSamples(0, 0, img.getWidth(), img.getHeight(), 2, pixels);
			case CHANNEL_4:
				return img.getRaster().getSamples(0, 0, img.getWidth(), img.getHeight(), 3, pixels);
			case CHANNEL_5:
				return img.getRaster().getSamples(0, 0, img.getWidth(), img.getHeight(), 4, pixels);
			case CHANNEL_6:
				return img.getRaster().getSamples(0, 0, img.getWidth(), img.getHeight(), 5, pixels);
			case CHANNEL_7:
				return img.getRaster().getSamples(0, 0, img.getWidth(), img.getHeight(), 6, pixels);
			case CHANNEL_8:
				return img.getRaster().getSamples(0, 0, img.getWidth(), img.getHeight(), 7, pixels);
			default:
				break;
			}
			
			return null;
		}
		
		
	}
	
	private static List<FeatureComputerBuilder> builders = Arrays.asList(new BasicFeatureComputerBuilder(), new HaralickFeatureComputerBuilder());
	
	

	public IntensityFeaturesPlugin(final ImageRegionStore<BufferedImage> regionServer) {
		this.regionStore = regionServer;
		
		params = new ParameterList();
		
		// Regions & resolution
		params.addTitleParameter("Resolution");
		params.addDoubleParameter("downsample", "Downsample", 1, null, "Amount to downsample the image before calculating textures; choose 1 to use full resolution, or a higher value to use a smaller image").
				addDoubleParameter("pixelSizeMicrons", "Preferred pixel size", 2, GeneralTools.micrometerSymbol(), "Preferred pixel size of the image used to calculate the tetures - higher values means coarser (lower resolution) images")
				;

		// Regions & resolution
		params.addTitleParameter("Regions");
		params.addChoiceParameter("region", "Region", RegionType.ROI, RegionType.values(), "The region within which to calculate the features");
		params.addDoubleParameter("tileSizeMicrons", "Tile diameter", 25, GeneralTools.micrometerSymbol(), "Diameter of tile around the object centroid used to calculate textures.\nOnly matters if tiles are being used (i.e. the region parameter isn't ROI).");
		params.addDoubleParameter("tileSizePixels", "Tile diameter", 200, "px (full resolution image)", "Diameter of tile around the object centroid used to calculate textures.\nOnly matters if tiles are being used (i.e. the region parameter isn't ROI).");

	}

	public IntensityFeaturesPlugin() {
		this(null);
	}
	
	
	
	@Override
	public boolean runPlugin(final PluginRunner<BufferedImage> pluginRunner, final String arg) {
		
		boolean tempRegionStore = false;
		if (regionStore == null) {
			regionStore = pluginRunner.getRegionStore();
			tempRegionStore = regionStore != null;
		}
		
		boolean success = super.runPlugin(pluginRunner, arg);
		
		pluginRunner.getHierarchy().fireHierarchyChangedEvent(this);
		
		if (tempRegionStore) {
			regionStore = null;
		}
		
		return success;
	}
	
	
	
	private static ImmutableDimension getPreferredTileSizePixels(final ImageServer<BufferedImage> server, final ParameterList params) {
		// Determine tile size
		int tileWidth, tileHeight;
		if (server.hasPixelSizeMicrons()) {
			double tileSize = params.getDoubleParameterValue("tileSizeMicrons");
			tileWidth = (int)(tileSize / server.getPixelWidthMicrons() + .5);
			tileHeight = (int)(tileSize / server.getPixelHeightMicrons() + .5);
		} else {
			tileWidth = (int)(params.getDoubleParameterValue("tileSizePx") + .5);
			tileHeight = tileWidth;
		}
		return new ImmutableDimension(tileWidth, tileHeight);
	}
	
	static String getDiameterString(final ImageServer<BufferedImage> server, final ParameterList params) {
		RegionType regionType = (RegionType)params.getChoiceParameterValue("region");
		String shape = regionType == RegionType.SQUARE ? "Square" : (regionType == RegionType.CIRCLE ? "Circle" : "ROI");
		String unit = server.hasPixelSizeMicrons() ? GeneralTools.micrometerSymbol() : "px";
		double pixelSize = server.hasPixelSizeMicrons() ? params.getDoubleParameterValue("pixelSizeMicrons") : params.getDoubleParameterValue("downsample");
		double regionSize = server.hasPixelSizeMicrons() ? params.getDoubleParameterValue("tileSizeMicrons") : params.getDoubleParameterValue("tileSizePixels");
		
		if (regionType == RegionType.ROI) {
			return String.format("ROI: %.2f %s per pixel", pixelSize, unit);
		} else {
			return String.format("%s: Diameter %.1f %s: %.2f %s per pixel", shape, regionSize, unit, pixelSize, unit);
		}
	}
	
	
	@Override
	protected void addRunnableTasks(final ImageData<BufferedImage> imageData, final PathObject parentObject, List<Runnable> tasks) {
		final ParameterList params = getParameterList(imageData);
		final ImageServer<BufferedImage> server = imageData.getServer();
		tasks.add(new IntensityFeatureRunnable(server, parentObject, params, imageData.getColorDeconvolutionStains(), regionStore));
	}
	
	
	@Override
	protected Collection<Runnable> getTasks(final PluginRunner<BufferedImage> runner) {
		Collection<Runnable> tasks = super.getTasks(runner);
		// If we have a region store, it can be preferable to shuffle the tasks for performance.
		// This is because regions larger than the requested tile size will be cached,
		// so threads waiting for adjacent tiles can both block waiting for the same image -
		// causing fetching regions to become a bottleneck.
		// By shuffling tiles, all the threads put in requests for different requests at the start
		// (which is slow), but as the image is processed then increasingly the required regions are
		// already in the cache when they are needed - causing a dramatic speedup during runtime.
		// Overall throughput should be improved, since the time spend blocked is minimized.
		// *However* this is only likely to work if the cache is sufficiently big... otherwise
		// a slowdown is possible, due to adjacent regions needing to be requested multiple times
		// because the cache has been emptied in the interim.
//		if (regionStore != null & Runtime.getRuntime().totalMemory() >= 1024L*1024L*1024L*4L) {
//			if (!(tasks instanceof List))
//				tasks = new ArrayList<>(tasks);
//			Collections.shuffle((List<?>)tasks);
			
		if (regionStore != null) {
			int n = tasks.size();
			Runnable[] tasks2 = new Runnable[n];
			if (rearrangeByStride(tasks, tasks2, Runtime.getRuntime().availableProcessors()))
				tasks = Arrays.asList(tasks2);
		}
		return tasks;
	}
	
	
	static class IntensityFeatureRunnable implements Runnable, TileListener<BufferedImage> {
		
		private ImageServer<BufferedImage> server;
		private ParameterList params;
		private PathObject parentObject;
		private ColorDeconvolutionStains stains;
		private ImageRegionStore<BufferedImage> store;
		
		public IntensityFeatureRunnable(final ImageServer<BufferedImage> server, final PathObject parentObject, final ParameterList params, final ColorDeconvolutionStains stains, final ImageRegionStore<BufferedImage> store) {
			this.server = server;
			this.parentObject = parentObject;
			this.params = params;
			this.store = store;
			this.stains = stains;
		}

		@Override
		public void tileAvailable(final String serverPath, final ImageRegion region, final BufferedImage tile) {}

		@Override
		public boolean requiresTileRegion(final String serverPath, final ImageRegion region) {
			return server != null && server.getPath().equals(serverPath);
		}

		@Override
		public void run() {
			try {
				if (store != null)
					store.addTileListener(this);
				processObject(parentObject, params, server, stains, store);
//			} catch (InterruptedException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
			} finally {
				if (store != null)
					store.removeTileListener(this);
				parentObject.getMeasurementList().closeList();
				
				server = null;
				params = null;
				store = null;
			}
		}
		
		
		@Override
		public String toString() {
			return "Intensity measurements";
		}
		
	}
	
	
	

	static boolean processObject(final PathObject pathObject, final ParameterList params, final ImageServer<BufferedImage> server, final ColorDeconvolutionStains stains, final ImageRegionStore<BufferedImage> regionStore) {

		// Determine amount to downsample
		double downsample;
		if (server.hasPixelSizeMicrons()) {
			downsample = params.getDoubleParameterValue("pixelSizeMicrons") / server.getAveragedPixelSizeMicrons();
		} else
			downsample = params.getDoubleParameterValue("downsample");

		// Determine region shape
		RegionType regionType = (RegionType)params.getChoiceParameterValue("region");
			
		// Try to get ROI
		ROI pathROI = pathObject.getROI();
//		if (pathObject instanceof PathCellObject && ((PathCellObject)pathObject).getNucleusROI() != null)
//			pathROI = ((PathCellObject)pathObject).getNucleusROI();
		if (pathROI == null)
			return false;
		
		// Get bounds
		RegionRequest region;
		if (regionType == RegionType.ROI) {
			region = RegionRequest.createInstance(server.getPath(), downsample, pathObject.getROI());
		} else {
			ImmutableDimension size = getPreferredTileSizePixels(server, params);
			//		RegionRequest region = RegionRequest.createInstance(server.getPath(), downsample, (int)(pathROI.getCentroidX() + .5) - size.width/2, (int)(pathROI.getCentroidY() + .5) - size.height/2, size.width, size.height, pathROI.getT(), pathROI.getZ());
			// Try to align with pixel boundaries according to the downsample being used - otherwise, interpolation can cause some strange, pattern artefacts
			int xStart = (int)((int)(pathROI.getCentroidX() / downsample + .5) * downsample) - size.width/2;
			int yStart = (int)((int)(pathROI.getCentroidY() / downsample + .5) * downsample) - size.height/2;
			int width = Math.min(server.getWidth(), xStart + size.width) - xStart;
			int height = Math.min(server.getHeight(), yStart + size.height) - yStart;
			region = RegionRequest.createInstance(server.getPath(), downsample, xStart, yStart, width, height, pathROI.getT(), pathROI.getZ());			
		}
		
		// Check image large enough to do *anything* of value
		if (region.getWidth() / downsample < 3 || region.getHeight() / downsample < 3)
			return false;

//		System.out.println(bounds);
//		System.out.println("Size: " + size);

		BufferedImage img = null;
		// Try to read the image using the ImageRegionServer... if this doesn't work out, fall back to using the default (slower) method
		if (regionStore != null) {
			try {
				img = regionStore.getImage(server, region);
			} catch (Exception e) {
//				if (e instanceof InterruptedException)
//					throw (InterruptedException)e;
				logger.info("Failed to read from {} in region store with request {}", server, region);
				e.printStackTrace();
			}
		}
		// Try again once more...
		if (img == null) {
			img = server.readBufferedImage(region);
		}
		if (img == null) {
			logger.error("Could not read image - unable to compute intensity features for {}", pathObject);
			return false;
		}

		// Create mask ROI if necessary
		byte[] maskBytes = null;
		if (regionType == RegionType.ROI) {
			ROI roi = pathObject.getROI();
			BufferedImage imgMask = BufferedImageTools.createROIMask(img.getWidth(), img.getHeight(), roi, region);
			maskBytes = ((DataBufferByte)imgMask.getRaster().getDataBuffer()).getData();
		}
		
		
		String prefix = getDiameterString(server, params);
		
		boolean isRGB = server.isRGB();
		int w = img.getWidth();
 		int h = img.getHeight();
		int[] rgbBuffer = isRGB ? img.getRGB(0, 0, w, h, null, 0, w) : null;
		float[] pixels = null;
		for (FeatureColorTransform transform : FeatureColorTransform.values()) {
			// Check if the color transform is requested
			if (Boolean.TRUE.equals(params.getBooleanParameterValue(transform.getKey()))) {
				
				// Transform the pixels
				pixels = transform.getTransformedPixels(img, rgbBuffer, stains, pixels);
				
				// Create the simple image
				FloatArraySimpleImage pixelImage = new FloatArraySimpleImage(pixels, w, h);
				
				// Apply any arbitrary mask
				if (maskBytes != null) {
					for (int i = 0; i < pixels.length; i++) {
						if (maskBytes[i] == (byte)0)
							pixelImage.setValue(i % w, i / w, Float.NaN);
					}
				} else if (regionType == RegionType.CIRCLE) {
					// Apply circular tile mask
					double cx = (w-1) / 2;
					double cy = (h-1) / 2;
					double radius = Math.max(w, h) * .5;
					double distThreshold = radius * radius;
					for (int y = 0; y < h; y++) {
						for (int x = 0; x < w; x++) {
							if ((cx - x)*(cx - x) + (cy - y)*(cy - y) > distThreshold)
								pixelImage.setValue(x, y, Float.NaN);
						}			
					}
				}
				
				// Do the computations
				for (FeatureComputerBuilder builder : builders) {
					FeatureComputer computer = builder.build();
					computer.updateFeatures(pixelImage, transform, params);
					// Add to the parent object
					String name = prefix + ": " + transform.getName(stains) + ": ";
					computer.addMeasurements(pathObject, name, params);
				}
			}
		}
		pathObject.getMeasurementList().closeList();
		if (pathObject instanceof PathAnnotationObject)
			((PathAnnotationObject)pathObject).setLocked(true);
		else if (pathObject instanceof TMACoreObject)
			((TMACoreObject)pathObject).setLocked(true);
		
		return true;
		
	}
	
	
	public ParameterList getDefaultParameterList(final ImageData<BufferedImage> imageData) {
		
		if (!parametersInitialized) {
			boolean hasMicrons = imageData.getServer().hasPixelSizeMicrons();
			
			params.getParameters().get("pixelSizeMicrons").setHidden(!hasMicrons);
			params.getParameters().get("downsample").setHidden(hasMicrons);
			
			params.getParameters().get("tileSizeMicrons").setHidden(!hasMicrons);
			params.getParameters().get("tileSizePixels").setHidden(hasMicrons);
			
			// Color transforms
			params.addTitleParameter("Color transforms");
			for (FeatureColorTransform transform : FeatureColorTransform.values()) {
				if (transform.supportsImage(imageData))
					params.addBooleanParameter(transform.getKey(), transform.getPrompt(imageData), false);
			}
	
			// Add feature-related parameters
			for (FeatureComputerBuilder builder : builders) {
				builder.addParameters(imageData, params);
			}
		}
		
		parametersInitialized = true;
		
		return params;
	}

	@Override
	public String getName() {
		return "Compute intensity features";
	}

	@Override
	public String getLastResultsDescription() {
		return "";
	}

	@Override
	public String getDescription() {
		return "Add Haralick texture features to existing object measurements";
	}

	@Override
	protected Collection<PathObject> getParentObjects(final PluginRunner<BufferedImage> runner) {
		return runner.getImageData().getHierarchy().getSelectionModel().getSelectedObjects();
	}

	@Override
	public Collection<Class<? extends PathObject>> getSupportedParentObjectClasses() {
		List<Class<? extends PathObject>> parents = new ArrayList<>();
		parents.add(PathDetectionObject.class);
		parents.add(PathAnnotationObject.class);
		parents.add(TMACoreObject.class);
		return parents;
	}
	
	
	
	
	
	
	
	
	
	
	static interface FeatureComputerBuilder {
		
		/**
		 * Add any required parameters (and title) to the ParameterList.
		 * 
		 * @param params
		 */
		public abstract void addParameters(final ImageData<?> imageData, final ParameterList params);

		/**
		 * Create a new FeatureComputer.
		 * 
		 * @return
		 */
		public abstract FeatureComputer build();
		
	}
	
	static interface FeatureComputer {
		
		/**
		 * Update the features.  This is used, rather than a compute features method, to support tiling - 
		 * i.e. it may be that only part of the image is passed at this stage.
		 * 
		 * @param img
		 * @param transform
		 * @param params
		 */
		public abstract void updateFeatures(final SimpleImage img, FeatureColorTransform transform, final ParameterList params);
		
		/**
		 * Add the final measurements to the specified object, calculated and updated previously.
		 * 
		 * The 'name' should encapsulate everything else that is required, i.e. color transform, resolution etc. 
		 * Therefore only the specific feature name needs to be appended to this.
		 * 
		 * @param pathObject
		 * @param name
		 * @param params
		 */
		public abstract void addMeasurements(final PathObject pathObject, final String name, final ParameterList params);
		
	}
	
	
	static class BasicFeatureComputer implements FeatureComputer {
		
		static enum Feature {
			MEAN("doMean", "Mean", "Compute mean intensity"),
			STD_DEV("doStdDev", "Standard deviation", "Compute standard deviation of intensities"),
			MIN_MAX("doMinMax", "Min & Max", "Compute minimum & maximum of intensities"),
//			SKEW_KURTOSIS("doSkewKurtosis", "Skew & Kurtosis", "Compute skew & kurtosis of intensities"),
			;
			
			private String key, prompt, help;
			
			Feature(String key, String prompt, String help) {
				this.key = key;
				this.prompt = prompt;
				this.help = help;
			}
			
		}

		private RunningStatistics stats;
		private HueStats hueStats;

		public void updateFeatures(final SimpleImage img,  FeatureColorTransform transform, final ParameterList params) {
			// Check if we need to do these computations at all
			boolean requireFeatures = false;
			for (Feature feature : Feature.values()) {
				if (Boolean.TRUE.equals(params.getBooleanParameterValue(feature.key))) {
					requireFeatures = true;
					break;
				}
			}
			if (!requireFeatures)
				return;
			
			// Handle Hue differently (due to its circular nature)
			if (transform == FeatureColorTransform.HUE) {
				if (hueStats == null)
					hueStats = new HueStats();
				hueStats.update(img);
				return;
			}
			
			// Update statistics
			if (stats == null)
				stats = new RunningStatistics();
			StatisticsHelper.updateRunningStatistics(stats, img);
		}

		@Override
		public void addMeasurements(PathObject pathObject, String name, ParameterList params) {
			// Handle Hue special case
			if (hueStats != null) {
				pathObject.getMeasurementList().putMeasurement(name + " Mean", hueStats.getMeanHue());
				return;
			}
			
			// Handle everything else
			if (stats == null)
				return;
			
			MeasurementList measurementList = pathObject.getMeasurementList();
			if (params.getBooleanParameterValue(Feature.MEAN.key))
				measurementList.putMeasurement(name + " Mean", stats.getMean());
			if (params.getBooleanParameterValue(Feature.STD_DEV.key))
				measurementList.putMeasurement(name + " Std.dev.", stats.getStdDev());
			if (params.getBooleanParameterValue(Feature.MIN_MAX.key)) {
				measurementList.putMeasurement(name + " Min", stats.getMin());
				measurementList.putMeasurement(name + " Max", stats.getMax());
			}
			
			logger.trace("Measured pixel count: {}", stats.nPixels());
			
		}
		
	}
	
	
	static class BasicFeatureComputerBuilder implements FeatureComputerBuilder {
		
		@Override
		public void addParameters(ImageData<?> imageData, ParameterList params) {
			params.addTitleParameter("Basic features");
			for (Feature feature : Arrays.asList(Feature.MEAN, Feature.STD_DEV, Feature.MIN_MAX)) {
				params.addBooleanParameter(feature.key, feature.prompt, false, feature.help);
			}
		}
		
		@Override
		public FeatureComputer build() {
			return new BasicFeatureComputer();
		}
		
	}
	
	
	
	static class HaralickFeaturesComp implements FeatureComputer {
		
		private CoocurranceMatrices matrices;

		@Override
		public void updateFeatures(SimpleImage img, FeatureColorTransform transform, ParameterList params) {
			if (!Boolean.TRUE.equals(params.getBooleanParameterValue("doHaralick")))
				return;
			
			// Don't compute results for Hue - would be confusing...
			if (transform == FeatureColorTransform.HUE)
				return;
			
			double[] minMax = transform.getHaralickMinMax();
			if (minMax == null) {
				minMax = new double[]{params.getDoubleParameterValue("haralickMin"), params.getDoubleParameterValue("haralickMax")};
			}
			
			int d = params.getIntParameterValue("haralickDistance");
			int nBins = params.getIntParameterValue("haralickBins");
			
			matrices = HaralickFeatureComputer.updateCooccurrenceMatrices(matrices, img, null, nBins, minMax[0], minMax[1], d);
		}

		@Override
		public void addMeasurements(PathObject pathObject, String name, ParameterList params) {
			if (matrices == null)
				return;
			
			MeasurementList measurementList = pathObject.getMeasurementList();
			HaralickFeatures haralickFeatures = matrices.getMeanFeatures();
			for (int i = 0; i < haralickFeatures.nFeatures(); i++) {
				measurementList.putMeasurement(String.format("%s Haralick %s (F%d)", name,
						haralickFeatures.getFeatureName(i),
						i), haralickFeatures.getFeature(i));
			}
		}

	}
	
	
	static class HaralickFeatureComputerBuilder implements FeatureComputerBuilder {

		@Override
		public void addParameters(ImageData<?> imageData, ParameterList params) {
			params.addTitleParameter("Haralick features");
			params.addBooleanParameter("doHaralick", "Compute Haralick features", false);
			
			if (!imageData.getServer().isRGB()) {
				params.addDoubleParameter("haralickMin", "Haralick min", Double.NaN)
						.addDoubleParameter("haralickMax", "Haralick max", Double.NaN);
			}
			params.addIntParameter("haralickDistance", "Haralick distance", 1, null, "Spacing between pixels used in computing the co-occurrence matrix for Haralick textures (default = 1)")
					.addIntParameter("haralickBins", "Haralick number of bins", 32, null, 8, 256, "Number of intensity bins to use when computing the co-occurrence matrix for Haralick textures (default = 32)");
			
		}

		@Override
		public FeatureComputer build() {
			return new HaralickFeaturesComp();
		}
		
	}
	
	
	/**
	 * Calculate the mean Hue.
	 * 
	 * Given that Hue values are 'circular', this is angle-based rather than a straightforward mean.
	 *
	 */
	static class HueStats {

		private double sinX = 0;
		private double cosX = 0;
		
		public void update(final SimpleImage img) {
			// Handle Hue differently (due to its circular nature)
			for (int y = 0; y < img.getHeight(); y++) {
				for (int x = 0; x < img.getWidth(); x++) {
					float val = img.getValue(x, y);
					if (Float.isNaN(val))
						continue;
					double alpha = val * 2 * Math.PI;
					sinX += Math.sin(alpha);
					cosX += Math.cos(alpha);
				}				
			}
		}
		
		public double getMeanHue() {
			return Math.atan2(sinX, cosX) / (2 * Math.PI) + 0.5;
		}
		
	}
	
	
	
}
