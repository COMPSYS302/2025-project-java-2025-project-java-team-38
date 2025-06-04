package com.example.closet;

import com.google.firebase.Timestamp;
import java.util.List;

public class ClothingItem {
    public String Care;
    public String Category;
    public String Fabric;
    public String Fit;
    public long Likes;
    public String Name;
    public long Views;
    public List<String>Sizes;
    public List<String>images;
    public Timestamp dateAdded;


    public ClothingItem() {}

    public String getName() { return Name; }
    public String getCategory() { return Category; }
    public String getFabric() { return Fabric; }
    public String getFit() { return Fit; }
    public String getCare() { return Care; }
    public long getLikes() { return Likes; }
    public long getViews() { return Views; }
    public List<String> getSizes() { return Sizes; }
    public List<String> getImages() { return images; }
    public Timestamp getDateAdded() { return dateAdded; }


}
