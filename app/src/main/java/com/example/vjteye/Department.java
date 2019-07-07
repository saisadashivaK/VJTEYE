package com.example.vjteye;


import java.util.LinkedList;
public class Department {
    private String name;
    private int dep_id;
    private static int count = 0;
    public LinkedList<Site> landmarks = new LinkedList<Site>();
    public static int getCount() {
        return count;
    }



    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDep_id() {
        return dep_id;
    }

    public void setDep_id(int dep_id) {
        this.dep_id = dep_id;
    }
    public void addSite(Site site){
        landmarks.add(site);
    }
//    public Site getSites

}
