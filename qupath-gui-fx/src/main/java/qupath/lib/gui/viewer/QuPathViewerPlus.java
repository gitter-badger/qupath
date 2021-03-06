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

package qupath.lib.gui.viewer;


import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import qupath.lib.gui.helpers.ColorToolsFX;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.stores.DefaultImageRegionStore;

/**
 * A whole slide viewer with optional extras... i.e. an overview, scalebar, location string...
 * 
 * @author Pete Bankhead
 */
public class QuPathViewerPlus extends QuPathViewer {

	private ViewerPlusDisplayOptions viewerDisplayOptions;
	
	private AnchorPane basePane = new AnchorPane();
	
	private ImageOverview overview = new ImageOverview(this);
	private Scalebar scalebar = new Scalebar(this);

	private BorderPane panelLocation = new BorderPane();
	private Label labelLocation = new Label(" ");
	private boolean useMicronsForLocationString = true;
	private Slider sliderZ = new Slider(0, 1, 0);
	private Slider sliderT = new Slider(0, 1, 0);

	private int padding = 10;
	
	
	public QuPathViewerPlus(final ImageData<BufferedImage> imageData, final DefaultImageRegionStore regionStore, final OverlayOptions overlayOptions, final ViewerPlusDisplayOptions viewerDisplayOptions) {
		super(imageData, regionStore, overlayOptions);
		
		
		sliderZ.setOrientation(Orientation.VERTICAL);
		sliderT.setOrientation(Orientation.HORIZONTAL);
		
		
		
		Pane view = super.getView();
		view.getChildren().add(basePane);
		
		basePane.prefWidthProperty().bind(view.widthProperty());
		basePane.prefHeightProperty().bind(view.heightProperty());
		view.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_MOVED, e -> {
			updateLocationString();
		});
		
//		view.widthProperty().addListener((e, f, g) -> {
//			System.err.println("Base width: " + basePane.getWidth() + "; View width: " + view.getWidth());
//		});
		
//		basePane.minHeightProperty().bind(view.heightProperty());
//		basePane.minHeightProperty().bind(view.heightProperty());

		
		// Add the overview (preview image for navigation)
		if (imageData != null)
			overview.imageDataChanged(this, null, imageData);
		Node overviewNode = overview.getNode();
		basePane.getChildren().add(overviewNode);
		AnchorPane.setTopAnchor(overviewNode, (double)padding);
		AnchorPane.setRightAnchor(overviewNode, (double)padding);

		// Add the location label
		labelLocation.setTextFill(Color.WHITE);
		labelLocation.setTextAlignment(TextAlignment.CENTER);
		labelLocation.setStyle("-fx-font-size: 0.8em;");
		panelLocation.setStyle("-fx-background-color: rgba(0, 0, 0, 0.4);");
		panelLocation.setMinSize(140, 40);
		panelLocation.setCenter(labelLocation);
		basePane.getChildren().add(panelLocation);
		AnchorPane.setBottomAnchor(panelLocation, (double)padding);
		AnchorPane.setRightAnchor(panelLocation, (double)padding);
		
		// Add the scalebar label
//		Node scalebarNode = PanelToolsFX.createSwingNode(scalebar);
		Node scalebarNode = scalebar.getNode();
		basePane.getChildren().add(scalebarNode);
		AnchorPane.setBottomAnchor(scalebarNode, (double)padding);
		AnchorPane.setLeftAnchor(scalebarNode, (double)padding);
		
		// Add the z-slider
		sliderZ.valueProperty().bindBidirectional(zPositionProperty());
//		sliderZ.setOpaque(false);
		sliderZ.setVisible(false);
		sliderZ.setTooltip(new Tooltip("Change z-slice"));
		AnchorPane.setLeftAnchor(sliderZ, (double)padding);
		AnchorPane.setTopAnchor(sliderZ, (double)padding);

		
		// Add the t-slider
		sliderT.valueProperty().bindBidirectional(tPositionProperty());
//		sliderT.setOpaque(false);
		sliderT.setVisible(false);
		sliderT.setTooltip(new Tooltip("Change time point"));
		
		AnchorPane.setLeftAnchor(sliderT, padding*2.0);
		AnchorPane.setTopAnchor(sliderT, 0.0);


		basePane.getChildren().addAll(sliderZ, sliderT);

		
		updateSliders();
		
		
		
		this.viewerDisplayOptions = viewerDisplayOptions;
		
		setLocationVisible(viewerDisplayOptions.getShowLocation());
		setOverviewVisible(viewerDisplayOptions.getShowOverview());
		setScalebarVisible(viewerDisplayOptions.getShowScalebar());
		
		viewerDisplayOptions.showLocationProperty().addListener((e, o, n) -> setLocationVisible(n));
		viewerDisplayOptions.showOverviewProperty().addListener((e, o, n) -> setOverviewVisible(n));
		viewerDisplayOptions.showScalebarProperty().addListener((e, o, n) -> setScalebarVisible(n));
	}
	
	
	private void updateSliders() {
		if (sliderZ == null || sliderT == null)
			return;
		ImageServer<?> server = getServer();
		if (server != null && server.nZSlices() > 1) {
			setSliderRange(sliderZ, getZPosition(), 0, server.nZSlices()-1);
			sliderZ.setVisible(true);
		} else
			sliderZ.setVisible(false);	
		
		if (server != null && server.nTimepoints() > 1) {
			setSliderRange(sliderT, getTPosition(), 0, server.nTimepoints()-1);
			sliderT.setVisible(true);
		} else
			sliderT.setVisible(false);	
		
		
//		sliderZ.setVisible(true);	
//		sliderT.setVisible(true);	

	}
	
	
	static void setSliderRange(final Slider slider, double position, double min, double max) {
		slider.setMin(min);
		slider.setMax(max);
		slider.setMajorTickUnit(1);
		slider.setValue(position);
	}
	
	
	@Override
	public void initializeForServer(ImageServer<BufferedImage> server) {
		super.initializeForServer(server);
		updateSliders();
	}


	private void setLocationVisible(boolean showLocation) {
		panelLocation.setVisible(showLocation);
	}

	public boolean isLocationVisible() {
		return panelLocation.isVisible();
	}

	private void setScalebarVisible(boolean scalebarVisible) {
		scalebar.setVisible(scalebarVisible);
	}

	public boolean isScalebarVisible() {
		return scalebar.isVisible();
	}

	private void setOverviewVisible(boolean overviewVisible) {
		overview.setVisible(overviewVisible);
	}

	public boolean isOverviewVisible() {
		return overview.isVisible();
	}


	// TODO: Make location string protected?
	void updateLocationString() {
		String s = null;
		if (labelLocation != null && hasServer())
			s = getLocationString(useMicronsForLocationString);
		if (s != null && s.length() > 0) {
			labelLocation.setText(s);
			panelLocation.setOpacity(1);
		} else {
			panelLocation.setOpacity(0);
		}
	}



	@Override
	void paintCanvas() {
		boolean imageWasUpdated = imageUpdated || locationUpdated;
		
		super.paintCanvas();
		
//		super.paintComponent(g);
//		// Draw bottom panel background
//		if (panelLocation.isVisible() && labelLocation.getText().length() > 0) {
//			g.setColor(DisplayHelpers.TRANSLUCENT_BLACK);
//			Rectangle bounds = panelLocation.getBounds();
//			int pad = 0;
//			g.fillRect(bounds.x-pad, bounds.y-pad, bounds.width+pad*2, bounds.height+pad*2);
//		}
		
		// Ensure the scalebar color is set, if required
		Bounds boundsFX = scalebar.getNode().getBoundsInParent();
		Rectangle2D bounds = new Rectangle2D.Double(boundsFX.getMinX(), boundsFX.getMinY(), boundsFX.getMaxX(), boundsFX.getMaxY());
		if (autoRecolorGridAndScalebar && imageWasUpdated) {
			if (getDisplayedClipShape(bounds).intersects(0, 0, getServerWidth(), getServerHeight())) {
				scalebar.setTextColor(getSuggestedOverlayColorFX());
			}
			else {
				scalebar.setTextColor(ColorToolsFX.TRANSLUCENT_WHITE_FX);
			}
		}
		
//		if (autoRecolorGridAndScalebar && colorOverlaySuggested != null) {
//			Rectangle bounds = labelLocation.getBounds();
//			scalebar.setForeground(getSuggestedOverlayColor(bounds.x, bounds.y, bounds.width, bounds.height));
////			scalebar.setForeground(colorOverlaySuggested);
//		}
	}
	
	public ViewerPlusDisplayOptions getViewerDisplayOptions() {
		return viewerDisplayOptions;
	}

	
//	@Override
//	public Pane getView() {
//		return basePane;
//	}
	

	@Override
	public void setDownsampleFactor(double downsampleFactor, double cx, double cy) {
		super.setDownsampleFactor(downsampleFactor, cx, cy);
		updateLocationString();
	}
	
	@Override
	public void repaintEntireImage() {
		super.repaintEntireImage();
		overview.repaint();
	}
	
	
}
