package io.github.hummelhose.desksharing.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "office")
public class Office {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "layout_width", nullable = false)
    private Integer layoutWidth;

    @Column(name = "layout_height", nullable = false)
    private Integer layoutHeight;

    protected Office() {
    }

    public Office(String name,
                  String description,
                  boolean active,
                  Integer layoutWidth,
                  Integer layoutHeight) {
        this.name = name;
        this.description = description;
        this.active = active;
        this.layoutWidth = layoutWidth;
        this.layoutHeight = layoutHeight;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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