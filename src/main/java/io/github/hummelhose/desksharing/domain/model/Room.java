package io.github.hummelhose.desksharing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "office_id")
    private Office office;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "pos_x")
    private Integer posX;

    @Column(name = "pos_y")
    private Integer posY;

    @Column(name = "layout_width", nullable = false)
    private Integer layoutWidth;

    @Column(name = "layout_height", nullable = false)
    private Integer layoutHeight;

    protected Room() {
    }

    public Room(String name,
                String description,
                boolean active,
                Integer layoutWidth,
                Integer layoutHeight) {
        this.name = name;
        this.description = description;
        this.active = active;
        this.posX = 0;
        this.posY = 0;
        this.layoutWidth = layoutWidth;
        this.layoutHeight = layoutHeight;
    }

    public Room(Office office,
                String name,
                String description,
                boolean active,
                Integer posX,
                Integer posY,
                Integer layoutWidth,
                Integer layoutHeight) {
        this.office = office;
        this.name = name;
        this.description = description;
        this.active = active;
        this.posX = posX;
        this.posY = posY;
        this.layoutWidth = layoutWidth;
        this.layoutHeight = layoutHeight;
    }

    public Long getId() {
        return id;
    }

    public Office getOffice() {
        return office;
    }

    public void setOffice(Office office) {
        this.office = office;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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

    public Integer getLayoutWidth() {
        return layoutWidth;
    }

    public void setLayoutWidth(Integer layoutWidth) {
        this.layoutWidth = layoutWidth;
    }

    public Integer getLayoutHeight() {
        return layoutHeight;
    }

    public void setLayoutHeight(Integer layoutHeight) {
        this.layoutHeight = layoutHeight;
    }
}