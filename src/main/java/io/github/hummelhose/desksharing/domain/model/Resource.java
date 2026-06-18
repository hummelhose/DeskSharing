package io.github.hummelhose.desksharing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "resource")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private boolean bookable;

    @Column(name = "pos_x")
    private Integer posX;

    @Column(name = "pos_y")
    private Integer posY;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    protected Resource() {
    }

    public Resource(String name,
                    String description,
                    ResourceType resourceType,
                    Room room,
                    boolean active,
                    boolean bookable,
                    Integer posX,
                    Integer posY) {
        this.name = name;
        this.description = description;
        this.resourceType = resourceType;
        this.room = room;
        this.active = active;
        this.bookable = bookable;
        this.posX = posX;
        this.posY = posY;
        this.width = 108;
        this.height = 74;
    }

    public Resource(String name,
                    String description,
                    ResourceType resourceType,
                    Room room,
                    boolean active,
                    boolean bookable,
                    Integer posX,
                    Integer posY,
                    Integer width,
                    Integer height) {
        this.name = name;
        this.description = description;
        this.resourceType = resourceType;
        this.room = room;
        this.active = active;
        this.bookable = bookable;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isBookable() {
        return bookable;
    }

    public void setBookable(boolean bookable) {
        this.bookable = bookable;
    }

    public Integer getPosX() {
        return posX;
    }

    public void setPosX(Integer posX) {
        this.posX = posX;
    }

    public Integer getPosY() {
        return posY;
    }

    public void setPosY(Integer posY) {
        this.posY = posY;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}