import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToolBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Created by Scott Cagno.
 * Copyright Cagno Solutions. All rights reserved.
 */

public class PaddleBall extends Application {

	private static final String STYLESHEET = "/css/style.css";
	private static final int H_BLOCKS = 10;
	private static final int V_BLOCKS = 5;
	private static final int TOTAL_BLOCKS = H_BLOCKS * V_BLOCKS;

	private final DoubleProperty ballX = new SimpleDoubleProperty();
	private final DoubleProperty ballY = new SimpleDoubleProperty();
	private final DoubleProperty paddleX = new SimpleDoubleProperty();
	private final BooleanProperty gameStopped = new SimpleBooleanProperty();
	private final BooleanProperty gameLost = new SimpleBooleanProperty(false);
	private final BooleanProperty gameWon = new SimpleBooleanProperty(false);
	private final DoubleProperty boxesLeft = new SimpleDoubleProperty(TOTAL_BLOCKS);

	private boolean movingDown = true;
	private boolean movingRight = true;
	private double movingSpeed = 1.0;
	private double paddleDragX = 0.0;
	private double paddleTranslateX = 0.0;

	private static final Image BLOCK = new Image(PaddleBall.class.getResourceAsStream("/img/block.png"));

	private final DropShadow dropShadow = new DropShadow();
	private final Rectangle borderTop = new Rectangle(500, 2);
	private final Rectangle borderBottom = new Rectangle(500, 2);
	private final Rectangle borderLeft = new Rectangle(2, 500);
	private final Rectangle borderRight = new Rectangle(2, 500);
	private final Circle ball = new Circle(10.0, Color.BLACK);
	private final Rectangle paddle = new Rectangle(200, 460, 100, 15);
	private final Text gameOverText = new Text("Game Over");
	private final Text gameWonText = new Text("You've won!!!");
	private final Button startButton = new Button("Start");
	private final Button quitButton = new Button("Quit");

	private final ProgressBar progressBar = new ProgressBar(100);
	private final Label remainingBlocksLabel = new Label();
	private final ToolBar toolbar = new ToolBar();

	private final Group area = new Group();
	private Timeline heartbeat = null;

	private final ObservableList<ImageView> boxes = FXCollections.observableArrayList();

	public PaddleBall() {

		// drop shadow setup
		dropShadow.setOffsetX(0.5);
		dropShadow.setOffsetY(4.0);
		dropShadow.setColor(Color.BLACK);

		// box borders setup
		borderTop.setX(0);
		borderTop.setY(30);
		borderTop.setEffect(dropShadow);
		borderBottom.setX(0);
		borderBottom.setY(500);
		borderLeft.setX(0);
		borderLeft.setY(0);
		borderRight.setX(498);
		borderRight.setY(0);

		// ball setup
		ball.setEffect(dropShadow);

		// paddle setup
		paddle.setLayoutX(20);
		paddle.setEffect(dropShadow);
		paddle.setFill(Color.RED);
		paddle.setCursor(Cursor.DISAPPEAR);
		paddle.setOnMousePressed(onMousePressed);
		paddle.setOnMouseDragged(onMouseDragged);

		// game text states
		gameOverText.setFont(Font.font("Arial", 36.0));
		gameOverText.setFill(Color.RED);
		gameOverText.setLayoutX(150);
		gameOverText.setLayoutY(330);
		gameOverText.setEffect(dropShadow);
		gameWonText.setFont(Font.font("Arial", 36.0));
		gameWonText.setFill(Color.GREEN);
		gameWonText.setLayoutX(150);
		gameWonText.setLayoutY(330);
		gameWonText.setEffect(dropShadow);

		// game start and stop events
		startButton.setOnAction(onStart);
		quitButton.setOnAction(onQuit);

		// toolbars setup
		toolbar.setMinWidth(500);
		toolbar.getItems().setAll(startButton, quitButton, progressBar, remainingBlocksLabel);

		// group area setup
		area.setFocusTraversable(true);
		area.getChildren().setAll(ball, borderTop, borderBottom, borderLeft, borderRight,
				paddle, gameOverText, gameWonText, toolbar);


		// heartbeat / event loop setup
		heartbeat = new Timeline(new KeyFrame(new Duration(10.0), pulseEvent));
		heartbeat.setCycleCount(Timeline.INDEFINITE);

	}


	/**
	 * Event Handlers
	 */
	private final EventHandler<MouseEvent> onMousePressed = new EventHandler<MouseEvent>() {
		public void handle(MouseEvent event) {
			paddleTranslateX = paddle.getTranslateX() + 150;
			paddleDragX = event.getSceneX();
		}
	};

	private final EventHandler<MouseEvent> onMouseDragged = new EventHandler<MouseEvent>() {
		public void handle(MouseEvent event) {
			if (!gameStopped.get()) {
				double x = paddleTranslateX + event.getSceneX() - paddleDragX;
				paddleX.setValue(x);
			}
		}
	};

	private final EventHandler<ActionEvent> onStart = new EventHandler<ActionEvent>() {
		public void handle(ActionEvent event) {
			initGame();
			gameStopped.set(false);
			heartbeat.playFromStart();
		}
	};

	private final EventHandler<ActionEvent> onQuit = new EventHandler<ActionEvent>() {
		public void handle(ActionEvent event) {
			Platform.exit();
		}
	};

	private final EventHandler<ActionEvent> pulseEvent = new EventHandler<ActionEvent>() {
		public void handle(final ActionEvent evt) {
			checkWin();
			checkCollisions();
			double x = movingRight ? movingSpeed : -movingSpeed;
			double y = movingDown ? movingSpeed : -movingSpeed;
			ballX.set(ballX.get() + x);
			ballY.set(ballY.get() + y);
		}
	};


	/**
	 *	Implemented start to fulfill Application interface
	 */
	public void start(final Stage stage) throws Exception {
		initGui(stage);
		initGame();
	}

	/**
	 * Protected methods (collision detection & check win)
	 */

	protected void checkWin() {
		if (0 == boxesLeft.get()) {
			gameWon.set(true);
			gameStopped.set(true);
			heartbeat.stop();
		}
	}

	protected void checkCollisions() {
		checkBoxCollisions();
		if (ball.intersects(paddle.getBoundsInLocal())) {
			incrementSpeed();
			movingDown = false;
		}
		if (ball.intersects(borderTop.getBoundsInLocal())) {
			incrementSpeed();
			movingDown = true;
		}
		if (ball.intersects(borderBottom.getBoundsInLocal())) {
			gameStopped.set(true);
			gameLost.set(true);
			heartbeat.stop();
		}
		if (ball.intersects(borderLeft.getBoundsInLocal())) {
			incrementSpeed();
			movingRight = true;
		}
		if (ball.intersects(borderRight.getBoundsInLocal())) {
			incrementSpeed();
			movingRight = false;
		}
		if (paddle.intersects(borderRight.getBoundsInLocal())) {
			paddleX.set(350);
		}
		if (paddle.intersects(borderLeft.getBoundsInLocal())) {
			paddleX.set(0);
		}
	}


	/**
	 * Private methods
	 */

	private void checkBoxCollisions() {
		for (ImageView r : boxes) {
			if (r.isVisible() && ball.intersects(r.getBoundsInParent())) {
				boxesLeft.set(boxesLeft.get() - 1);
				r.setVisible(false);
			}
		}
	}

	private void incrementSpeed() {
		if (movingSpeed <= 6)
			movingSpeed += movingSpeed * 0.5;
	}

	private void initGame() {
		boxesLeft.set(TOTAL_BLOCKS);
		for (ImageView r : boxes) {
			r.setVisible(true);
		}
		movingSpeed = 1.0;
		movingDown = true;
		movingRight = true;
		ballX.setValue(250);
		ballY.setValue(350);
		paddleX.setValue(175);
		startButton.disableProperty().bind(gameStopped.not());
		ball.centerXProperty().bind(ballX);
		ball.centerYProperty().bind(ballY);
		paddle.xProperty().bind(paddleX);
		gameStopped.set(true);
		gameLost.set(false);
		gameOverText.visibleProperty().bind(gameLost);
		gameWon.set(false);
		gameWonText.visibleProperty().bind(gameWon);
		area.requestFocus();
		progressBar.progressProperty().bind(
				boxesLeft.subtract(TOTAL_BLOCKS).multiply(-1)
						.divide(TOTAL_BLOCKS));
		remainingBlocksLabel.textProperty().bind(
				Bindings.format("%.0f boxes left", boxesLeft));
	}

	private void initBoxes() {
		int startX = 15;
		int startY = 30;
		for (int v = 1; v <= V_BLOCKS; v++) {
			for (int h = 1; h <= H_BLOCKS; h++) {
				int x = startX + (h * 40);
				int y = startY + (v * 40);
				ImageView imageView = new ImageView(BLOCK);
				imageView.setLayoutX(x);
				imageView.setLayoutY(y);
				boxes.add(imageView);
			}
		}
		area.getChildren().addAll(boxes);
	}

	private void initGui(final Stage stage) {
		Scene scene = new Scene(area, 500, 530, Color.GRAY);
		initBoxes();
		stage.setScene(scene);
		stage.setTitle("Paddle Ball");
		stage.getIcons().add(BLOCK);
		scene.getStylesheets().add(STYLESHEET);
		stage.show();
	}

	public static void main(final String... args) {
		Application.launch(args);
	}
}
