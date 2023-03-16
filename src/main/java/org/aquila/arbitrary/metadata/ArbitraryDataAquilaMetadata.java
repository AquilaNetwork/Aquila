package org.aquila.arbitrary.metadata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aquila.repository.DataException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * ArbitraryDataAquilaMetadata
 *
 * This is a base class to handle reading and writing JSON to a .aquila folder
 * within the supplied filePath. This is used when storing data against an existing
 * arbitrary data file structure.
 *
 * It is not usable on its own; it must be subclassed, with three methods overridden:
 *
 * fileName() - the file name to use within the .aquila folder
 * readJson() - code to unserialize the JSON file
 * buildJson() - code to serialize the JSON file
 *
 */
public class ArbitraryDataAquilaMetadata extends ArbitraryDataMetadata {

    protected static final Logger LOGGER = LogManager.getLogger(ArbitraryDataAquilaMetadata.class);

    protected Path filePath;
    protected Path aquilaDirectoryPath;

    protected String jsonString;

    public ArbitraryDataAquilaMetadata(Path filePath) {
        super(filePath);

        this.aquilaDirectoryPath = Paths.get(filePath.toString(), ".aquila");
    }

    protected String fileName() {
        // To be overridden
        return null;
    }

    protected void readJson() throws DataException {
        // To be overridden
    }

    protected void buildJson() {
        // To be overridden
    }


    @Override
    public void read() throws IOException, DataException {
        this.loadJson();
        this.readJson();
    }

    @Override
    public void write() throws IOException, DataException {
        this.buildJson();
        this.createParentDirectories();
        this.createAquilaDirectory();

        Path patchPath = Paths.get(this.aquilaDirectoryPath.toString(), this.fileName());
        BufferedWriter writer = new BufferedWriter(new FileWriter(patchPath.toString()));
        writer.write(this.jsonString);
        writer.newLine();
        writer.close();
    }

    @Override
    protected void loadJson() throws IOException {
        Path path = Paths.get(this.aquilaDirectoryPath.toString(), this.fileName());
        File patchFile = new File(path.toString());
        if (!patchFile.exists()) {
            throw new IOException(String.format("Patch file doesn't exist: %s", path.toString()));
        }

        this.jsonString = new String(Files.readAllBytes(path));
    }


    protected void createAquilaDirectory() throws DataException {
        try {
            Files.createDirectories(this.aquilaDirectoryPath);
        } catch (IOException e) {
            throw new DataException("Unable to create .aquila directory");
        }
    }


    public String getJsonString() {
        return this.jsonString;
    }

}
