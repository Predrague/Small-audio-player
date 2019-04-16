package player;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import javafx.scene.control.*;
import javafx.scene.control.Slider;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;

public class App implements Initializable {
    private Media sound;
    private static MediaPlayer mediaPlayer;
    private int songNumber;
    private boolean shuffleOn = false;

    @FXML private MediaView mediaView;
    @FXML private Slider sliderVolume;
    @FXML private Slider sliderProgress;
    @FXML private ListView lstPlaylist;
    @FXML private Label lblDuration;
    @FXML private Label lblCurrentTime;
    @FXML private Label lblSongName;
    @FXML private Label lblStatus;
    @FXML private Button btnPlay;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mediaView = new MediaView(mediaPlayer);

        lstPlaylist.setOnMouseClicked(new EventHandler<MouseEvent>() {                                                  //Double click on item in playlist
            @Override
            public void handle(MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        if(lstPlaylist.getItems() == null) return;                           //empty playlist
                        Object selected = lstPlaylist.getSelectionModel().getSelectedItem();
                        songNumber = lstPlaylist.getItems().indexOf(selected);
                        if(mediaPlayer != null) mediaPlayer.stop();
                        initializeSong(selected);
                        mediaPlayer.play();
                        btnPlay.setText("\u23F8");
                }
            }
        });


        lstPlaylist.setCellFactory(lv-> new ListCell<File>(){
            @Override
            protected void  updateItem(File file, boolean empty){                                         //Displaying only file name and not full path in playlist cell
                super.updateItem(file,empty);
                setText(file == null ? null : file.getName());
            }
        });


    }

    @FXML protected void openFiles(){
        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Audio files ", "*.mp3");   //TODO add other formats
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(filter);
        List<File> allFiles = fc.showOpenMultipleDialog(null);
        if(allFiles == null) return;
        lstPlaylist.getItems().addAll(allFiles);

    }

    @FXML protected void openFolder(){
        DirectoryChooser dc = new DirectoryChooser();
        File directory = dc.showDialog(null);
        if(directory == null) return;
        for (File f:directory.listFiles()) {
            if(f.getName().substring(f.getName().lastIndexOf('.')+1).equalsIgnoreCase("mp3")){         //Adding .mp3 files only
                lstPlaylist.getItems().add(f);
            }
        };
    }

    public void initializeSong(Object song){                                                                            //Setting sound file to media player, new instance of media player is needed every time
        File soundFile = new File(song.toString());
        sound = new Media(soundFile.toURI().toString());
        if(mediaPlayer != null) mediaPlayer.dispose();
        mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.volumeProperty().bindBidirectional(sliderVolume.valueProperty());                                   //Binding volume to volume slider value
        sliderProgress.setValue(0);

        lblStatus.textProperty().bind(mediaPlayer.statusProperty().asString());                                         //Binding status label (lblStatus) text property to status property of media player

        mediaPlayer.currentTimeProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                sliderProgress.setValue(mediaPlayer.getCurrentTime().toSeconds());
                String currentTime = mediaPlayer.getCurrentTime().toMinutes() + "";
                if (!(currentTime.length() < 4)) lblCurrentTime.setText(currentTime.substring(0, 4));
                if (mediaPlayer.getCurrentTime().toSeconds() >= sound.getDuration().toSeconds()) nextSong();
            }
        });

        sliderProgress.setOnMousePressed(MouseEvent  ->{
            mediaPlayer.seek(Duration.seconds(sliderProgress.getValue()));
        });

        mediaPlayer.setOnReady(new Runnable() {                                                                         //When player is set up changes progress bar and duration of song label values
            @Override
            public void run() {
                lblDuration.setText((sound.getDuration().toMinutes()+"").substring(0,4));
                sliderProgress.setMax(sound.getDuration().toSeconds());
            }
        });

        lblSongName.setText(soundFile.getName().substring(0, soundFile.getName().length() - 4));                        //Cuts .mp3 part from file name
    }

    public void playSong() {
        if(lstPlaylist.getItems().size() == 0) return;
        if (mediaPlayer != null) {
            switch (mediaPlayer.getStatus()) {
                case PAUSED:
                    btnPlay.setText("\u23F8");
                    mediaPlayer.play();
                    return;
                case PLAYING:
                    btnPlay.setText("►");
                    mediaPlayer.pause();
                    return;
                case STOPPED:
                    initializeSong(lstPlaylist.getSelectionModel().getSelectedItem());
                    songNumber = lstPlaylist.getSelectionModel().getSelectedIndex();
                    btnPlay.setText("\u23F8");
                    mediaPlayer.play();
                    return;
            }
        }
        Object selected = lstPlaylist.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        initializeSong(selected);
        songNumber = lstPlaylist.getSelectionModel().getSelectedIndex();
        mediaPlayer.play();
        btnPlay.setText("\u23F8");
    }

    @FXML protected void stopSong(){
        if(mediaPlayer == null) return;
        mediaPlayer.stop();
        btnPlay.setText("►");
        lblSongName.setText("");
    }

    @FXML protected void previousSong(){
        if(mediaPlayer == null) return;
        if(lstPlaylist.getItems().size() == 0) return;
        if(songNumber-1 ==-1) songNumber = lstPlaylist.getItems().size()-1;  else songNumber--;
        mediaPlayer.stop();
        initializeSong(lstPlaylist.getItems().get(songNumber));
        lstPlaylist.scrollTo(songNumber);
        mediaPlayer.play();
    }

    @FXML protected void nextSong(){
        if(mediaPlayer == null) return;
        if(lstPlaylist.getItems().size() == 0) return;
        if(shuffleOn){
            Random rnd = new Random();
            songNumber = rnd.nextInt(lstPlaylist.getItems().size());
        }else {
            if(songNumber+1 == lstPlaylist.getItems().size()) songNumber = 0; else songNumber++;
        }
        mediaPlayer.stop();
        initializeSong(lstPlaylist.getItems().get(songNumber));
        mediaPlayer.play();
    }

    @FXML protected void removeSong(){
        Object selected = lstPlaylist.getSelectionModel().getSelectedItem();
        lstPlaylist.getItems().remove(selected);
    }

    @FXML protected void changeShuffle(){
        if(shuffleOn) shuffleOn = false; else shuffleOn = true;
    }
}
