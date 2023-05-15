package com.example.mappingmemoriesapp.Models;

import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class PageLocation implements Parcelable{

    //Clase para almacenar las ubicaciones y las páginas del diario

    private GeoPoint geo_point;
    private @ServerTimestamp Date timestamp;
    private String user_id;
    private String title, text;
    private String image;

    //Constructores
    public PageLocation(String user_id, GeoPoint geo_point, Date timestamp, String title, String text, String image) {
        this.user_id = user_id;
        this.geo_point = geo_point;
        this.timestamp = timestamp;
        this.title = title;
        this.text = text;
        this.image = image;
    }

    public PageLocation() {}

    protected PageLocation(Parcel in) {
        geo_point = in.readParcelable(GeoPoint.class.getClassLoader());
        timestamp = (Date) in.readSerializable();
        user_id = in.readString();
        title = in.readString();
        text = in.readString();
        image = in.readString();
    }

    public static final Creator<PageLocation> CREATOR = new Creator<PageLocation>() {
        @Override
        public PageLocation createFromParcel(Parcel in) {
            return new PageLocation(in);
        }

        @Override
        public PageLocation[] newArray(int size) {
            return new PageLocation[size];
        }
    };

    //Getters & Setters
    public GeoPoint getGeo_point() {
        return geo_point;
    }

    public void setGeo_point(GeoPoint geo_point) {
        this.geo_point = geo_point;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    @Override
    public String toString() {
        return "PageLocation{" +
                "user_id=" + user_id +
                ", geo_point=" + geo_point +
                ", timestamp=" + timestamp +
                ", title=" + title +
                ", text=" + text +
                ", image=" + image +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable((Parcelable) geo_point, flags);
        dest.writeSerializable(timestamp);
        dest.writeString(user_id);
        dest.writeString(title);
        dest.writeString(text);
        dest.writeString(image);
    }

}
