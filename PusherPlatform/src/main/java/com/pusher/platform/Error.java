package com.pusher.platform;

/**
 * The base class for all Pusher Platform errors, including the ones for services built on top of it.
 * Currently this encapsulates the underlying exception, using its message and stack trace. It also allows specifying additional URL supporting the error.
 * */
public class Error extends Throwable {
    private final String url;

    public Error(Throwable throwable){
        this(throwable, null);
    }
    public Error(Throwable throwable, String url){
        super(throwable);
        this.url = url;
    }

    /**
     * Gets the support URL associated with this particular error, if it exists.
     * @return URL supporting this error, empty String ("") otherwise.
     * */
    public String getUrl(){
        return url != null? url : "";
    }

    /**
     * Check whether there is a supporting URL associated with this particular error
     * @return true is there is an URL available, false otherwise.
     * */
    public boolean hasUrl(){
        return url != null;
    }
}
