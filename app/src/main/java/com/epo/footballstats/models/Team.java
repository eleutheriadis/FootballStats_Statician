package com.epo.footballstats.models;

public class Team {
    private String id;
    private String name;
    private String city;
    private String logoUrl;

    public Team() {}

    public Team(String id, String name, String city, String logoUrl) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.logoUrl = logoUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
