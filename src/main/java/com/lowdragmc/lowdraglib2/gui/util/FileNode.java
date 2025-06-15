package com.lowdragmc.lowdraglib2.gui.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

@EqualsAndHashCode
public class FileNode implements ITreeNode<File, Void> {
    @Getter
    public final int dimension;
    @Getter
    public final File key;
    @Nullable
    @Setter
    @Accessors(chain = true)
    protected Predicate<FileNode> valid;

    public FileNode(File dir){
        this(0, dir);
    }

    private FileNode(int dimension, File key) {
        this.dimension = dimension;
        this.key = key;
    }

    @Override
    public @Nullable Void getContent() {
        return null;
    }

    @Override
    public boolean isLeaf() {
        return key.isFile();
    }

    @Override
    @Nonnull
    public List<FileNode> getChildren() {
        var children = new ArrayList<FileNode>();
        var files = key.listFiles();
        if (files != null) {
            Arrays.stream(files).sorted((a, b)->{
                if (a.isFile() && b.isFile()) {
                    return a.compareTo(b);
                } else if (a.isDirectory() && b.isDirectory()) {
                    return a.compareTo(b);
                } else if(a.isDirectory()) {
                    return -1;
                }
                return 1;
            }).forEach(file -> children.add(new FileNode(dimension + 1, file).setValid(valid)));
        }
        return children;
    }

    @Override
    public String toString() {
        return getKey().getName();
    }
}
