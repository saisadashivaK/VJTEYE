package com.example.vjteye;

class Site {
    private String name;
    private int id;
    private String info;
    private int unique_id;
    private static int count;

    public static int getCount() {
        return count;
    }
    public static void setCount(int count){
        Site.count = count;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUnique_id() {
        return unique_id;
    }

    public void setUnique_id(int unique_id) {
        this.unique_id = unique_id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
