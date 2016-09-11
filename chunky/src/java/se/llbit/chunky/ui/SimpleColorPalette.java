/*
 * Copyright (c) 2016 Jesper Öqvist <jesper@llbit.se>
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.llbit.chunky.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import se.llbit.math.Vector4;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

/**
 * Color palette for the simple color picker.
 * The color palette contains a HSV-gradient displaying a slice of the HSV color cube.
 * The color palette also shows several color swatches.
 */
public class SimpleColorPalette extends Region implements Initializable {

  private static final Image gradientImage;
  static {
    List<Vector4> gradient = new ArrayList<>();
    gradient.add(new Vector4(1, 0, 0, 0.00));
    gradient.add(new Vector4(1, 1, 0, 1 / 6.0));
    gradient.add(new Vector4(0, 1, 0, 2 / 6.0));
    gradient.add(new Vector4(0, 1, 1, 3 / 6.0));
    gradient.add(new Vector4(0, 0, 1, 4 / 6.0));
    gradient.add(new Vector4(1, 0, 1, 5 / 6.0));
    gradient.add(new Vector4(1, 0, 0, 1.00));
    gradientImage = GradientEditor.drawGradient(522, 75, gradient);
  }

  private final SimpleColorPicker colorPicker;
  private Random random = new Random();

  private @FXML VBox palette;
  private @FXML ImageView huePicker;
  private @FXML Canvas colorSample;
  private @FXML TextField webColorCode;
  private @FXML Button saveBtn;
  private @FXML Button cancelBtn;
  private @FXML StackPane satValueGrid;
  private @FXML Pane huePickerOverlay;
  private @FXML Region sample0;
  private @FXML Region sample1;
  private @FXML Region sample2;
  private @FXML Region sample3;
  private @FXML Region sample4;
  private @FXML Region history0;
  private @FXML Region history1;
  private @FXML Region history2;
  private @FXML Region history3;
  private @FXML Region history4;

  private final DoubleProperty hue = new SimpleDoubleProperty();
  private final DoubleProperty saturation = new SimpleDoubleProperty();
  private final DoubleProperty value = new SimpleDoubleProperty();

  private final Circle satValIndicator = new Circle(9);
  private final Rectangle hueIndicator = new Rectangle(20, 69);

  private Region[] sample;
  private Region[] history;

  public SimpleColorPalette(SimpleColorPicker colorPicker) {
    this.colorPicker = colorPicker;
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("ColorPicker.fxml"));
      // We need to explicitly set the class loader because in Java 1.8u40 FXMLLoader has a null
      // class loader for some reason:
      loader.setClassLoader(getClass().getClassLoader());
      loader.setController(this);
      getChildren().add(loader.load());
      setOnMouseClicked(event -> event.consume());
    } catch (IOException e) {
      throw new Error(e);
    }
  }


  @Override public void initialize(URL location, ResourceBundle resources) {
    sample = new Region[] { sample0, sample1, sample2, sample3, sample4 };
    history = new Region[] { history0, history1, history2, history3, history4 };

    // Handle color selection on click.
    colorSample.setOnMouseClicked(event -> {
      colorPicker.updateHistory();
      colorPicker.hide();
    });

    webColorCode.textProperty().addListener((observable, oldValue, newValue) -> {
      try {
        changeColor(Color.web(newValue));
      } catch (IllegalArgumentException e) {
        // Harmless exception - ignored.
      }
    });

    saveBtn.setOnAction(event -> {
      colorPicker.updateHistory();
      colorPicker.hide();
    });

    saveBtn.setDefaultButton(true);

    cancelBtn.setOnAction(event -> {
      colorPicker.revertToOriginalColor();
      colorPicker.hide();
    });

    satValueGrid.setBackground(
        new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
    satValueGrid.backgroundProperty().bind(new ObjectBinding<Background>() {
      {
        bind(hue);
      }

      @Override protected Background computeValue() {
        return new Background(
            new BackgroundFill(Color.hsb(hue.get() * 360, 1.0, 1.0), CornerRadii.EMPTY,
                Insets.EMPTY));
      }
    });

    Pane saturationOverlay = new Pane();
    saturationOverlay.setBackground(new Background(new BackgroundFill(
        new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 255, 255, 1.0)), new Stop(1, Color.rgb(255, 255, 255, 0.0))),
        CornerRadii.EMPTY, Insets.EMPTY)));

    Pane valueOverlay = new Pane();
    valueOverlay.setBackground(new Background(new BackgroundFill(
        new LinearGradient(0, 1, 0, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(0, 0, 0, 1.0)), new Stop(1, Color.rgb(0, 0, 0, 0.0))),
        CornerRadii.EMPTY, Insets.EMPTY)));

    satValIndicator.layoutXProperty().bind(saturation.multiply(256));
    satValIndicator.layoutYProperty().bind(Bindings.subtract(1, value).multiply(256));
    satValIndicator.setStroke(Color.WHITE);
    satValIndicator.fillProperty().bind(new ObjectBinding<Paint>() {
      {
        bind(hue);
        bind(saturation);
        bind(value);
      }

      @Override protected Paint computeValue() {
        return Color.hsb(hue.get() * 360, saturation.get(), value.get());
      }
    });
    satValIndicator.setStrokeWidth(2);
    satValIndicator.setMouseTransparent(true);
    satValIndicator.setEffect(new DropShadow(5, Color.BLACK));

    hueIndicator.setMouseTransparent(true);
    hueIndicator.setTranslateX(-10);
    hueIndicator.setTranslateY(3);
    hueIndicator.layoutXProperty().bind(hue.multiply(huePicker.fitWidthProperty()));
    hueIndicator.fillProperty().bind(new ObjectBinding<Paint>() {
      {
        bind(hue);
      }

      @Override protected Paint computeValue() {
        return Color.hsb(hue.get() * 360, 1.0, 1.0);
      }
    });
    hueIndicator.setStroke(Color.WHITE);
    hueIndicator.setStrokeWidth(2);
    hueIndicator.setEffect(new DropShadow(5, Color.BLACK));

    huePickerOverlay.getChildren().add(hueIndicator);
    huePickerOverlay.setClip(new Rectangle(522, 75));

    valueOverlay.getChildren().add(satValIndicator);
    valueOverlay.setClip(new Rectangle(256, 256)); // Clip the indicator circle.

    satValueGrid.getChildren().addAll(saturationOverlay, valueOverlay);

    setBackground(new Background(
        new BackgroundFill(Color.rgb(240, 240, 240), new CornerRadii(4.0), new Insets(0))));
    DropShadow dropShadow = new DropShadow();
    dropShadow.setColor(Color.color(0, 0, 0, 0.8));
    dropShadow.setWidth(18);
    dropShadow.setHeight(18);
    setEffect(dropShadow);

    setHueGradient();

    EventHandler<MouseEvent> hueMouseHandler =
        event -> hue.set(clamp(event.getX() / huePicker.getFitWidth()));

    huePickerOverlay.setOnMouseDragged(hueMouseHandler);
    huePickerOverlay.setOnMousePressed(hueMouseHandler);

    EventHandler<MouseEvent> mouseHandler = event -> {
      saturation.set(clamp(event.getX() / satValueGrid.getWidth()));
      value.set(clamp(1 - event.getY() / satValueGrid.getHeight()));
    };

    valueOverlay.setOnMousePressed(mouseHandler);
    valueOverlay.setOnMouseDragged(mouseHandler);

    hue.addListener((observable, oldValue, newValue) -> updateCurrentColor(newValue.doubleValue(),
        saturation.get(), value.get()));
    saturation.addListener(
        (observable, oldValue, newValue) -> updateCurrentColor(hue.get(), newValue.doubleValue(),
            value.get()));
    value.addListener(
        (observable, oldValue, newValue) -> updateCurrentColor(hue.get(), saturation.get(),
            newValue.doubleValue()));

    EventHandler<MouseEvent> swatchClickHandler = event -> {
      if (event.getSource() instanceof Region) {
        Region swatch = (Region) event.getSource();
        if (!swatch.getBackground().getFills().isEmpty()) {
          changeColor((Color) swatch.getBackground().getFills().get(0).getFill());
        }
      }
    };

    for (Region region : history) {
      region.setOnMouseClicked(swatchClickHandler);
    }

    for (Region region : sample) {
      region.setOnMouseClicked(swatchClickHandler);
    }
    // Initialize history with random colors.
    for (int i = 0; i < history.length; ++i) {
      history[i].setBackground(
          new Background(new BackgroundFill(getRandomNearColor(colorPicker.getColor()),
              CornerRadii.EMPTY, Insets.EMPTY)));
    }
  }

  protected void setColor(Color color) {
    hue.set(color.getHue() / 360);
    saturation.set(color.getSaturation());
    value.set(color.getBrightness());
    updateCurrentColor(color);
  }

  protected void addToHistory(Color color) {
    for (int i = history.length - 1; i >= 1; i -= 1) {
      history[i].setBackground(history[i - 1].getBackground());
    }
    history[0].setBackground(new Background(
        new BackgroundFill(color, CornerRadii.EMPTY, Insets.EMPTY)));
  }

  private void setHueGradient() {
    huePicker.setImage(gradientImage);
  }

  private static double clamp(double value) {
    return (value < 0) ? 0 : (value > 1 ? 1 : value);
  }

  private void updateCurrentColor(double hue, double saturation, double value) {
    updateCurrentColor(Color.hsb(hue * 360, saturation, value));
  }

  private void updateCurrentColor(Color newColor) {
    for (int i = 0; i < sample.length; ++i) {
      sample[i].setBackground(
          new Background(new BackgroundFill(getRandomNearColor(newColor),
              CornerRadii.EMPTY, Insets.EMPTY)));
    }
    changeColor(newColor);
    // TODO: make sure color values are rounded correctly.
    webColorCode.setText(String.format("#%02X%02X%02X", (int) (newColor.getRed() * 255 + 0.5),
        (int) (newColor.getGreen() * 255 + 0.5), (int) (newColor.getBlue() * 255 + 0.5)));
  }

  private void changeColor(Color newColor) {
    colorPicker.setColor(newColor);

    GraphicsContext gc = colorSample.getGraphicsContext2D();
    gc.setFill(newColor);
    gc.fillRect(0, 0, colorSample.getWidth(), colorSample.getHeight());
  }

  private Color getRandomNearColor(Color color) {
    double hueMod = random.nextDouble() * .45;
    double satMod = random.nextDouble() * .75;
    double valMod = random.nextDouble() * .4;
    hueMod = 2 * (random.nextDouble() - .5) * 360 * hueMod * hueMod;
    satMod = 2 * (random.nextDouble() - .5) * satMod * satMod;
    valMod = 2 * (random.nextDouble() - .5) * valMod * valMod;
    double hue = color.getHue() + hueMod;
    double sat = Math.max(0, Math.min(1, color.getSaturation() + satMod));
    double val = Math.max(0, Math.min(1, color.getBrightness() + valMod));
    if (hue > 360) {
      hue -= 360;
    } else if (hue < 0) {
      hue += 360;
    }
    return Color.hsb(hue, sat, val);
  }

}