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

package com.sun.internals.controls;

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
 * Internal, CSS-styleable circular spinner (two rotating arcs) used as the
 * thumbnail-cell loading indicator. Not part of any public API.
 *
 * <p>Themeable through CSS: {@code -nfx-radius}, {@code -nfx-color},
 * {@code -nfx-stroke-width}, {@code -nfx-cycle-duration}. The
 * {@link #autoStartProperty() autoStart} flag is the on/off switch.</p>
 *
 * @author XDSSWAR
 */
public final class NfxCircularLoader extends StackPane {
    private static final String STYLE_CLASS = "nfx-circular-loader";

    private final Arc arc1;
    private final Arc arc2;
    private final Group arcGroup;
    private boolean playing = false;
    private ParallelTransition parallelTransition;

    /**
     * Builds the loader and (when {@code autoStart}) begins spinning.
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

        colorProperty().addListener(o -> handleChange());
        radiusProperty().addListener(o -> handleChange());
        strokeWidthProperty().addListener(o -> handleChange());
        cycleDurationProperty().addListener(o -> handleChange());

        autoStartProperty().addListener((obs, o, yes) -> {
            if (yes) {
                start();
                setVisible(true);
            } else {
                stop();
                setVisible(false);
            }
        });
        if (isAutoStart()) {
            setVisible(true);
            start();
        }
    }

    private void handleChange() {
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
        if (wasPlaying) {
            start();
        }
    }

    private DoubleProperty radius;

    /** @return the arc radius */
    public double getRadius() {
        return radiusProperty().get();
    }

    /** @return the radius property */
    public DoubleProperty radiusProperty() {
        if (radius == null) {
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

    /** @param radius the new arc radius */
    public void setRadius(double radius) {
        radiusProperty().set(radius);
    }

    private ObjectProperty<Paint> color;

    /** @return the stroke color */
    public Paint getColor() {
        return colorProperty().get();
    }

    /** @return the color property */
    public ObjectProperty<Paint> colorProperty() {
        if (color == null) {
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

    /** @param color the new stroke color */
    public void setColor(Paint color) {
        colorProperty().set(color);
    }

    private DoubleProperty strokeWidth;

    /** @return the arc stroke width */
    public double getStrokeWidth() {
        return strokeWidthProperty().get();
    }

    /** @return the stroke width property */
    public DoubleProperty strokeWidthProperty() {
        if (strokeWidth == null) {
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

    /** @param strokeWidth the new arc stroke width */
    public void setStrokeWidth(double strokeWidth) {
        strokeWidthProperty().set(strokeWidth);
    }

    private DoubleProperty cycleDuration;

    /** @return one-rotation duration, in seconds */
    public double getCycleDuration() {
        return cycleDurationProperty().get();
    }

    /** @return the cycle duration property */
    public DoubleProperty cycleDurationProperty() {
        if (cycleDuration == null) {
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

    /** @param cycleDuration the new one-rotation duration, in seconds */
    public void setCycleDuration(double cycleDuration) {
        cycleDurationProperty().set(cycleDuration);
    }

    private BooleanProperty autoStart;

    /** @return whether the loader auto-starts */
    public boolean isAutoStart() {
        return autoStartProperty().get();
    }

    /** @return the auto-start property */
    public BooleanProperty autoStartProperty() {
        if (autoStart == null) {
            autoStart = new SimpleBooleanProperty(this, "autoStart", true);
        }
        return autoStart;
    }

    /** @param autoStart whether the loader should auto-start (and show) */
    public void setAutoStart(boolean autoStart) {
        autoStartProperty().set(autoStart);
    }

    private void setupTransitions() {
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(getCycleDuration()), arcGroup);
        rotateTransition.setByAngle(360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setInterpolator(Interpolator.LINEAR);
        parallelTransition = new ParallelTransition(rotateTransition);
        parallelTransition.setCycleCount(Animation.INDEFINITE);
    }

    private void start() {
        if (parallelTransition != null && parallelTransition.getStatus() != Animation.Status.RUNNING) {
            parallelTransition.play();
        }
        playing = true;
    }

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
    private static final class Styleables {
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
