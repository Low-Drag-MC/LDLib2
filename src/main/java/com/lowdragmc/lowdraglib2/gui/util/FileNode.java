package com.lowdragmc.lowdraglib2.gui.util;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class FileNode implements ITreeNode<File, Void> {
    @Nullable
    @Getter
    public final FileNode parent;
    @Getter
    public final int dimension;
    @Getter
    public final File key;
    @Nullable
    @Setter
    @Accessors(chain = true)
    protected Predicate<FileNode> valid;

    public FileNode(File dir){
        this(null, 0, dir);
    }

    private FileNode(@Nullable FileNode parent, int dimension, File key) {
        this.parent = parent;
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
            }).forEach(file -> {
                var node = new FileNode(this, dimension + 1, file);
                if (valid != null && !valid.test(node)) return;
                children.add(node.setValid(valid));
            });
        }
        return children;
    }

    @Override
    public String toString() {
        return getKey().getName();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileNode fileNode = (FileNode) o;
        return dimension == fileNode.dimension &&
                Objects.equals(key, fileNode.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, key);
    }
}
