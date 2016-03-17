package br.org.funcate.terramobile.model.domain;

/**
 * Created by Andre Carvalho on 01/06/15.
 */
public class Project {

    /**
     * These attributes refers to states of the GeoPackage.
     */
    public static final int NEW=0;// New packages downloaded from server.
    public static final int UPLOAD=1;// Packages gathering data to sending to server.
    //public static final int SYNC=2;// Packages whose data was synchronized with the server (never exist in app).

    private Integer id;
    private String name;
    private String filePath;
    private int downloaded;
    private int updated;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name=name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int isUpdated() {
        return updated;
    }

    public void setUpdated(int updated) {
        this.updated = updated;
    }

    @Override
    public String toString() {
        return name.substring(0, name.indexOf('.'));
    }

    public int isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(int downloaded) {
        this.downloaded = downloaded;
    }

}