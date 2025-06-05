package com.example.closet;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model class representing a clothing item.
 * Used with Firebase Firestore for data persistence.
 */
public class ClothingItem {

    private String id;
    private String name;
    private String category;
    private String fabric;
    private String fit;
    private String care;

    // Firestore field: "images" (List of URL strings)
    private List<String> images;

    // Firestore field: "likedUsers" (List of UIDs who have liked this item)
    private List<String> likedUsers;

    // Firestore fields for tracking inventory/metrics
    private List<String> sizes;
    private int views;
    private int likes;

    // Firestore field: "dateAdded" (will be a Timestamp in Firestore)
    private com.google.firebase.Timestamp dateAdded;

    // This field is not stored in Firestore—it’s set locally based on the current user.
    private boolean likedByCurrentUser = false;

    /** Default constructor required for Firestore deserialization */
    public ClothingItem() {
        // Initialize lists so we don’t get NullPointerExceptions
        this.images = new ArrayList<>();
        this.likedUsers = new ArrayList<>();
        this.sizes = new ArrayList<>();
    }

    /**
     * (Optional) Convenience constructor
     */
    public ClothingItem(String name,
                        String category,
                        String fabric,
                        String fit,
                        String care,
                        List<String> images,
                        List<String> sizes) {
        this.name = name;
        this.category = category;
        this.fabric = fabric;
        this.fit = fit;
        this.care = care;
        this.images = (images != null ? images : new ArrayList<>());
        this.sizes = (sizes != null ? sizes : new ArrayList<>());
        this.likedUsers = new ArrayList<>();
        this.views = 0;
        this.likes = 0;

    }

    /** ID getter/setter (not annotated—FireStore uses document ID, not a field) */
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /** Firestore field: "Name" */
    @PropertyName("Name")
    public String getName() {
        return name;
    }

    @PropertyName("Name")
    public void setName(String name) {
        this.name = name;
    }

    /** Firestore field: "Category" */
    @PropertyName("Category")
    public String getCategory() {
        return category;
    }

    @PropertyName("Category")
    public void setCategory(String category) {
        this.category = category;
    }

    /** Firestore field: "Fabric" */
    @PropertyName("Fabric")
    public String getFabric() {
        return fabric;
    }

    @PropertyName("Fabric")
    public void setFabric(String fabric) {
        this.fabric = fabric;
    }

    /** Firestore field: "Fit" */
    @PropertyName("Fit")
    public String getFit() {
        return fit;
    }

    @PropertyName("Fit")
    public void setFit(String fit) {
        this.fit = fit;
    }

    /** Firestore field: "Care" */
    @PropertyName("Care")
    public String getCare() {
        return care;
    }

    @PropertyName("Care")
    public void setCare(String care) {
        this.care = care;
    }

    /** Firestore field: "Images" (an array of URL strings) */
    @PropertyName("Images")
    public List<String> getImages() {
        return images;
    }

    @PropertyName("Images")
    public void setImages(List<String> images) {
        this.images = images;
    }

    /** Firestore field: "likedUsers" (list of UIDs who have liked this item) */
    @PropertyName("likedUsers")
    public List<String> getLikedUsers() {
        return likedUsers;
    }

    @PropertyName("likedUsers")
    public void setLikedUsers(List<String> likedUsers) {
        this.likedUsers = likedUsers;
    }

    /** Firestore field: "Sizes" (list of strings) */
    @PropertyName("Sizes")
    public List<String> getSizes() {
        return sizes;
    }

    @PropertyName("Sizes")
    public void setSizes(List<String> sizes) {
        this.sizes = sizes;
    }

    /** Firestore field: "Views" (an integer) */
    @PropertyName("Views")
    public int getViews() {
        return views;
    }

    @PropertyName("Views")
    public void setViews(int views) {
        this.views = views;
    }

    /** Firestore field: "Likes" (an integer) */
    @PropertyName("Likes")
    public int getLikes() {
        return likes;
    }

    @PropertyName("Likes")
    public void setLikes(int likes) {
        this.likes = likes;
    }

    //
    public com.google.firebase.Timestamp getDateAdded() {
        return dateAdded;
    }

    //
    public void setDateAdded(com.google.firebase.Timestamp dateAdded) {
        this.dateAdded = dateAdded;
    }

    /** Not stored in Firestore—tracks whether the current user has liked this item */
    public boolean isLikedByCurrentUser() {
        return likedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        this.likedByCurrentUser = likedByCurrentUser;
    }

    @Override
    public String toString() {
        return "ClothingItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", fabric='" + fabric + '\'' +
                ", fit='" + fit + '\'' +
                ", care='" + care + '\'' +
                ", images=" + images +
                ", likedUsers=" + likedUsers +
                ", sizes=" + sizes +
                ", views=" + views +
                ", likes=" + likes +
                ", dateAdded=" + dateAdded +
                ", likedByCurrentUser=" + likedByCurrentUser +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClothingItem that = (ClothingItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return (id != null) ? id.hashCode() : 0;
    }
}
