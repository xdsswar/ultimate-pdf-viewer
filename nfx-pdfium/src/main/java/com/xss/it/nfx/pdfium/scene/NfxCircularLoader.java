/*
 * Copyright © 2025. XTREME SOFTWARE SOLUTIONS
 *
 * All rights reserved. Unauthorized use, reproduction, or distribution
 * of this software or any portion of it is strictly prohibited and may
 * result in severe civil and criminal penalties. This code is the sole
 * proprietary of XTREME SOFTWARE SOLUTIONS.
 *
 * Commercialization, redistribution, and use without explicit permission
 * from XTREME SOFTWARE SOLUTIONS, are expressly forbidden.
 */

package com.xss.it.nfx.pdfium.scene;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.*;
import javafx.css.converter.PaintConverter;
import javafx.css.converter.SizeConverter;
import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Internal, fully CSS-styleable circular spinner: two rounded arcs rotate
 * continuously to form a spinning ring. Used by {@code PdfPageView}'s per-page
 * loader; never part of the public API.
 *
 * <p>Themeable through CSS:</p>
 * <ul>
 *     <li>{@code -nfx-radius} — arc radius (default {@code 20}).</li>
 *     <li>{@code -nfx-color} — stroke color (default red).</li>
 *     <li>{@code -nfx-stroke-width} — arc thickness (default {@code 4}).</li>
 *     <li>{@code -nfx-cycle-duration} — one rotation, in seconds (default {@code 1.2}).</li>
 * </ul>
 *
 * <p>The {@link #autoStartProperty() autoStart} flag (default {@code true}) is the on/off
 * switch: {@code true} starts the spin and shows the node, {@code false} stops it and hides
 * the node — so the animation never runs while hidden.</p>
 *
 * @author XDSSWAR
 */
public final class NfxCircularLoader extends StackPane {
    /**
     * Style class
     */
    private static final String STYLE_CLASS = "nfx-circular-loader";

    /**
     * The first arc element used in the loader animation.
     */
    private final Arc arc1;

    /**
     * The second arc element used in the loader animation.
     */
    private final Arc arc2;

    /**
     * A group containing both arcs, used to coordinate their animation and transformations.
     */
    private final Group arcGroup;

    /**
     * State of animations
     */
    private boolean playing = false;

    /**
     * A parallel transition that synchronizes multiple animations for the loader.
     */
    private ParallelTransition parallelTransition;

    /**
     * Constructs an instance of the {@code NfxCircularLoader}, initializing the circular arcs and animation settings.
     */
    public NfxCircularLoader() {
        super();
        setVisible(false);
        setPrefWidth(Region.USE_COMPUTED_SIZE);
        setPrefHeight(Region.USE_COMPUTED_SIZE);
        getStyleClass().add(STYLE_CLASS);
        setMaxWidth(3 * getRadius());
        arc1 = new Arc(0, 0, getRadius(), getRadius(), 0, 120);
        arc1.setType(ArcType.OPEN);
        arc1.setStroke(getColor());
        arc1.setStrokeWidth(getStrokeWidth());
        arc1.setFill(Color.TRANSPARENT);
        arc1.setStrokeLineCap(StrokeLineCap.ROUND);

        arc2 = new Arc(0, 0, getRadius(), getRadius(), 0, 120);
        arc2.setType(ArcType.OPEN);
        arc2.setStroke(getColor());
        arc2.setStrokeWidth(getStrokeWidth());
        arc2.setFill(Color.TRANSPARENT);
        arc2.setStrokeLineCap(StrokeLineCap.ROUND);

        arc2.setStartAngle(180);
        arcGroup = new Group(arc1, arc2);
        this.getChildren().add(arcGroup);
        setupTransitions();

        colorProperty().addListener(o->handleChange());
        radiusProperty().addListener(o->handleChange());
        strokeWidthProperty().addListener(o->handleChange());
        cycleDurationProperty().addListener(o->handleChange());

        /*
         * AutoStart
         */
        autoStartProperty().addListener((obs, o, yes) -> {
            if (yes){
                start();
                setVisible(true);
            }
            else {
                stop();
                setVisible(false);
            }
        });
        if (isAutoStart()){
            setVisible(true);
            start();
        }
    }

    /**
     * Updates the loader's arcs to reflect the latest property values, such as color, radius, and arch width.
     */
    private void handleChange(){
        boolean wasPlaying = playing;
        stop();
        arc1.setStroke(getColor());
        arc1.setRadiusX(getRadius());
        arc1.setRadiusY(getRadius());
        arc1.setStrokeWidth(getStrokeWidth());
        arc2.setStroke(getColor());
        arc2.setRadiusX(getRadius());
        arc2.setRadiusY(getRadius());
        arc2.setStrokeWidth(getStrokeWidth());
        setupTransitions();
        if (wasPlaying){
            start();
        }
    }

    /**
     * Radius property for controlling the size of the circles in the loader animation.
     */
    private DoubleProperty radius;

    /**
     * Gets the current radius value of the circles.
     *
     * @return the current radius of the circles
     */
    public double getRadius() {
        return radiusProperty().get();
    }

    /**
     * Provides access to the radius property, allowing it to be bound or observed.
     *
     * @return the {@link DoubleProperty} representing the radius of the circles
     */
    public DoubleProperty radiusProperty() {
        if (radius == null){
            radius = new StyleableDoubleProperty(20) {
                @Override
                public Object getBean() {
                    return NfxCircularLoader.this;
                }

                @Override
                public String getName() {
                    return "radius";
                }

                @Override
                public CssMetaData<? extends Styleable, Number> getCssMetaData() {
                    return Styleables.RADIUS_STYLE;
                }
            };
        }
        return radius;
    }

    /**
     * Sets the radius of the circles in the loader animation.
     *
     * @param radius the new radius value for the circles
     */
    public void setRadius(double radius) {
        radiusProperty().set(radius);
    }


    /**
     * Property for controlling the color paint of the loader elements.
     */
    private ObjectProperty<Paint> color;

    /**
     * Gets the current color paint applied to the loader elements.
     *
     * @return the {@link Paint} currently set as the color of the loader elements
     */
    public Paint getColor() {
        return colorProperty().get();
    }

    /**
     * Provides access to the color property, allowing it to be bound or observed.
     *
     * @return the {@link ObjectProperty} representing the color paint of the loader elements
     */
    public ObjectProperty<Paint> colorProperty() {
        if (color == null){
            color = new StyleableObjectProperty<>(Color.RED) {
                @Override
                public Object getBean() {
                    return NfxCircularLoader.this;
                }

                @Override
                public String getName() {
                    return "color";
                }

                @Override
                public CssMetaData<? extends Styleable, Paint> getCssMetaData() {
                    return Styleables.COLOR_STYLE;
                }
            };
        }
        return color;
    }

    /**
     * Sets the color paint for the loader elements.
     *
     * @param color the new {@link Paint} to be applied as the color of the loader elements
     */
    public void setColor(Paint color) {
        colorProperty().set(color);
    }

    /**
     * Property for controlling the width of the loader's arch elements.
     */
    private DoubleProperty strokeWidth;

    /**
     * Gets the current width of the loader's arch elements.
     *
     * @return the width of the arch elements, in pixels
     */
    public double getStrokeWidth() {
        return strokeWidthProperty().get();
    }

    /**
     * Provides access to the arch width property, allowing it to be bound or observed.
     *
     * @return the {@link DoubleProperty} representing the width of the arch elements
     */
    public DoubleProperty strokeWidthProperty() {
        if (strokeWidth == null){
            strokeWidth = new StyleableDoubleProperty(4) {
                @Override
                public Object getBean() {
                    return NfxCircularLoader.this;
                }

                @Override
                public String getName() {
                    return "strokeWidth";
                }

                @Override
                public CssMetaData<? extends Styleable, Number> getCssMetaData() {
                    return Styleables.STROKE_WIDTH;
                }
            };
        }
        return strokeWidth;
    }

    /**
     * Sets the width of the loader's arch elements.
     *
     * @param strokeWidth the new width, in pixels, for the arch elements
     */
    public void setStrokeWidth(double strokeWidth) {
        strokeWidthProperty().set(strokeWidth);
    }

    /**
     * Property for controlling the duration of the loader's animation cycle.
     */
    private DoubleProperty cycleDuration;

    /**
     * Gets the current duration of the loader's animation cycle.
     *
     * @return the cycle duration, in milliseconds
     */
    public double getCycleDuration() {
        return cycleDurationProperty().get();
    }

    /**
     * Provides access to the cycle duration property, allowing it to be bound or observed.
     *
     * @return the {@link DoubleProperty} representing the duration of the loader's animation cycle
     */
    public DoubleProperty cycleDurationProperty() {
        if (cycleDuration == null){
            cycleDuration = new StyleableDoubleProperty(1.2) {
                @Override
                public Object getBean() {
                    return NfxCircularLoader.this;
                }

                @Override
                public String getName() {
                    return "cycleDuration";
                }

                @Override
                public CssMetaData<? extends Styleable, Number> getCssMetaData() {
                    return Styleables.CYCLE_STYLE;
                }
            };
        }
        return cycleDuration;
    }

    /**
     * Sets the duration of the loader's animation cycle.
     *
     * @param cycleDuration the new cycle duration, in milliseconds, for the animation
     */
    public void setCycleDuration(double cycleDuration) {
        cycleDurationProperty().set(cycleDuration);
    }

    /**
     * Property indicating whether the loader animation should start automatically.
     */
    private BooleanProperty autoStart;

    /**
     * Checks if the loader animation is set to start automatically.
     *
     * @return {@code true} if the animation should auto-start, {@code false} otherwise
     */
    public boolean isAutoStart() {
        return autoStartProperty().get();
    }

    /**
     * Provides access to the auto-start property, allowing it to be bound or observed.
     *
     * @return the {@link BooleanProperty} representing the auto-start setting
     */
    public BooleanProperty autoStartProperty() {
        if (autoStart ==  null){
            autoStart = new SimpleBooleanProperty(this, "autoStart", true);
        }
        return autoStart;
    }

    /**
     * Sets whether the loader animation should start automatically.
     *
     * @param autoStart {@code true} to enable auto-start, {@code false} to disable it
     */
    public void setAutoStart(boolean autoStart) {
        autoStartProperty().set(autoStart);
    }

    /**
     * Configures the rotation transition for the loader's arc group, creating a continuous spinning animation.
     */
    private void setupTransitions(){
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(getCycleDuration()), arcGroup);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        parallelTransition = new ParallelTransition(rotateTransition);
        parallelTransition.setCycleCount(Animation.INDEFINITE);
    }

    /**
     * Starts the loader animation.
     */
    private void start() {
        if (parallelTransition != null && parallelTransition.getStatus() != Animation.Status.RUNNING) {
            parallelTransition.play();
        }
        playing = true;
    }

    /**
     * Stops the loader animation.
     */
    private void stop() {
        if (parallelTransition != null) {
            parallelTransition.stop();
        }
        playing = false;
    }

    /**
     * CSS metadata for the styleable properties of {@code NfxCircularLoader}.
     */
    @SuppressWarnings("all")
    private static final class Styleables{
        /**
         * CSS metadata for the radius of the loader's circular elements ("-nfx-radius").
         */
        private static final CssMetaData<NfxCircularLoader, Number> RADIUS_STYLE = new CssMetaData<>(
                "-nfx-radius", SizeConverter.getInstance(), 20
        ) {
            @Override
            public boolean isSettable(NfxCircularLoader s) {
                return s.radiusProperty() == null || !s.radiusProperty().isBound();
            }


            @Override
            public StyleableProperty<Number> getStyleableProperty(NfxCircularLoader s) {
                return (StyleableProperty<Number>) s.radiusProperty();
            }
        };

        /**
         * CSS metadata for the color of the loader's circular elements ("-nfx-color").
         */
        private static final CssMetaData<NfxCircularLoader, Paint> COLOR_STYLE = new CssMetaData<>(
                "-nfx-color", PaintConverter.getInstance(), Color.RED
        ) {
            @Override
            public boolean isSettable(NfxCircularLoader s) {
                return s.colorProperty() == null || !s.colorProperty().isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(NfxCircularLoader s) {
                return (StyleableProperty<Paint>) s.colorProperty();
            }
        };

        /**
         * CSS metadata for the stroke width of the loader's circular elements ("-nfx-stroke-width").
         */
        private static final CssMetaData<NfxCircularLoader, Number> STROKE_WIDTH = new CssMetaData<>(
                "-nfx-stroke-width", SizeConverter.getInstance(), 4
        ) {
            @Override
            public boolean isSettable(NfxCircularLoader s) {
                return s.strokeWidthProperty() == null || !s.strokeWidthProperty().isBound();
            }


            @Override
            public StyleableProperty<Number> getStyleableProperty(NfxCircularLoader s) {
                return (StyleableProperty<Number>) s.strokeWidthProperty();
            }
        };

        /**
         * CSS metadata for the cycle duration of the loader's animation ("-nfx-cycle-duration").
         */
        private static final CssMetaData<NfxCircularLoader, Number> CYCLE_STYLE = new CssMetaData<>(
                "-nfx-cycle-duration", SizeConverter.getInstance(), 1.2
        ) {
            @Override
            public boolean isSettable(NfxCircularLoader s) {
                return s.cycleDurationProperty() == null || !s.cycleDurationProperty().isBound();
            }


            @Override
            public StyleableProperty<Number> getStyleableProperty(NfxCircularLoader s) {
                return (StyleableProperty<Number>) s.cycleDurationProperty();
            }
        };


        /**
         * All styleable properties for {@code NfxCircularLoader}, including inherited ones.
         */
        public static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(StackPane.getClassCssMetaData());
            styleables.add(RADIUS_STYLE);
            styleables.add(COLOR_STYLE);
            styleables.add(STROKE_WIDTH);
            styleables.add(CYCLE_STYLE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return Styleables.STYLEABLES;
    }
}
