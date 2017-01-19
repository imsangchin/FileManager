package com.asus.filetransfer.utility;

/**
 * Created by Yenju_Lai on 2016/1/29.
 */
public class HttpServerEvents {

    public enum Action {
        Upload,
        Download,
        Delete,
        CreateFolder,
        Compress,
        Browse
    }

    private String catalog = "WebPage";
    private Action action;
    private String label = null;

    public HttpServerEvents(Action action) {
        this.action = action;
    }

    public String getCatalog() {
        return catalog;
    }

    public Action getAction() {
        return action;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpServerEvents)) return false;

        HttpServerEvents that = (HttpServerEvents) o;

        /*/if (!catalog.equals(that.catalog)) return false;
        if (action != that.action) return false;
        return !(label != null ? !label.equals(that.label) : that.label != null);*/
        return action == that.action;
    }

    @Override
    public int hashCode() {
        /*int result = catalog.hashCode();
        result = 31 * result + action.hashCode();
        result = 31 * result + (label != null ? label.hashCode() : 0);*/
        return action.hashCode();
    }
}
