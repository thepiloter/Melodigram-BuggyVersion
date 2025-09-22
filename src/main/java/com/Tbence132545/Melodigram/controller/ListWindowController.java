// java
package com.Tbence132545.Melodigram.controller;

import com.Tbence132545.Melodigram.model.MidiFileService;
import com.Tbence132545.Melodigram.model.MidiInputSelector;
import com.Tbence132545.Melodigram.model.MidiPlayer;
import com.Tbence132545.Melodigram.view.ListWindow;
import com.Tbence132545.Melodigram.view.MainWindow;
import com.Tbence132545.Melodigram.view.PianoWindow;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class ListWindowController implements ListWindow.MidiFileActionListener {

    private final ListWindow view;
    private final MidiFileService midiFileService;

    public ListWindowController(ListWindow view) {
        this.view = view;
        this.midiFileService = new MidiFileService();
        setupEventListeners();
        loadAndDisplayMidiFiles();
    }

    private void setupEventListeners() {
        view.setBackButtonListener(e -> handleBackButton());
        view.setImportButtonListener(e -> handleImportButton());
    }

    private void loadAndDisplayMidiFiles() {
        String[] fileNames = midiFileService.getAllMidiFileNames().toArray(new String[0]);
        view.setMidiFileList(fileNames, this);
    }

    private void handleBackButton() {
        view.dispose();
        MainWindow mainWin = new MainWindow();
        new MainWindowController(mainWin).openMainWindow();
    }

    private void handleImportButton() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select a MIDI file to import");
        fileChooser.setFileFilter(new FileNameExtensionFilter("MIDI Files", "mid", "midi"));

        if (fileChooser.showOpenDialog(view) == JFileChooser.APPROVE_OPTION) {
            try {
                midiFileService.importMidiFile(fileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(view, "File imported successfully!");
                loadAndDisplayMidiFiles();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(view, "Could not import file: " + ex.getMessage(), "Import Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onAssignHandsClicked(String midiFilename) {
        String simpleFilename = midiFilename.replace("midi/", "");
        openPianoWindowForEditing(simpleFilename);
    }
    @Override
    public void onWatchAndListenClicked(String midiFilename) {
        String simpleFilename = midiFilename.replace("midi/", "");
        openPianoWindow(simpleFilename, false, null);
    }

    @Override
    public void onPracticeClicked(String midiFilename, HandMode mode) {
        MidiInputSelector selector = new MidiInputSelector(view, deviceInfo -> {
            if (deviceInfo != null) {
                openPianoWindow(midiFilename, true, mode, deviceInfo);
            }
        });
        SwingUtilities.invokeLater(() -> selector.setVisible(true));
    }
    private void openPianoWindowForEditing(String midiFileName) {
        SwingUtilities.invokeLater(() -> {
            try {
                MidiFileService.MidiData midiData = midiFileService.loadMidiData(midiFileName);
                int[] range = MidiPlayer.extractNoteRange(midiData.sequence());
                PianoWindow pianoWindow = new PianoWindow(range[0], range[1]);
                PlaybackController playbackController = new PlaybackController(midiData.player(), pianoWindow);

                playbackController.setEditingMode(true);

                pianoWindow.setBackButtonListener(e -> {
                    midiData.player().stop();
                    pianoWindow.dispose();
                    SwingUtilities.invokeLater(() -> view.setVisible(true));
                });

                pianoWindow.setVisible(true);
                SwingUtilities.invokeLater(() -> view.setVisible(false));

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(view, "Error Opening Editor:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    private void openPianoWindow(String midiFileName, boolean isPractice, HandMode hand, MidiDevice.Info... midiDeviceInfo) {
        SwingUtilities.invokeLater(() -> {
            MidiDevice inputDevice = null;
            try {
                MidiFileService.MidiData midiData = midiFileService.loadMidiData(midiFileName);

                int[] range = MidiPlayer.extractNoteRange(midiData.sequence());
                PianoWindow pianoWindow = new PianoWindow(range[0], range[1]);
                PlaybackController playbackController = new PlaybackController(midiData.player(), pianoWindow);

                if (isPractice) {
                    if (midiDeviceInfo.length == 0) throw new IllegalStateException("MIDI device info required for practice mode.");
                    inputDevice = MidiSystem.getMidiDevice(midiDeviceInfo[0]);
                    playbackController.setPracticeMode(true, hand);
                    playbackController.setMidiInputDevice(inputDevice);
                }

                final MidiDevice finalInputDevice = inputDevice;
                pianoWindow.setBackButtonListener(e -> {
                    midiData.player().stop();
                    if (finalInputDevice != null && finalInputDevice.isOpen()) {
                        finalInputDevice.close();
                    }
                    pianoWindow.dispose();
                    SwingUtilities.invokeLater(() -> view.setVisible(true));
                });

                pianoWindow.setVisible(true);
                SwingUtilities.invokeLater(() -> view.setVisible(false));

            } catch (Exception e) {
                e.printStackTrace();
                if (inputDevice != null && inputDevice.isOpen()) inputDevice.close();
                String errorTitle = isPractice ? "Error Initializing Practice" : "Error Opening Piano View";
                JOptionPane.showMessageDialog(view, errorTitle + ":\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}