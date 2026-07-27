-- Add database-level delete cascades for the office hierarchy.
--
-- office -> room -> resource -> reservation
--
-- Deleting an office automatically deletes its rooms, resources,
-- and reservations.
--
-- Deleting a room automatically deletes its resources and reservations.
--
-- Deleting a resource automatically deletes its reservations.

ALTER TABLE room
DROP CONSTRAINT fk_room_office;

ALTER TABLE room
    ADD CONSTRAINT fk_room_office
        FOREIGN KEY (office_id)
            REFERENCES office (id)
            ON DELETE CASCADE;


ALTER TABLE resource
DROP CONSTRAINT fk_resource_room;

ALTER TABLE resource
    ADD CONSTRAINT fk_resource_room
        FOREIGN KEY (room_id)
            REFERENCES room (id)
            ON DELETE CASCADE;


ALTER TABLE reservation
DROP CONSTRAINT fk_reservation_resource;

ALTER TABLE reservation
    ADD CONSTRAINT fk_reservation_resource
        FOREIGN KEY (resource_id)
            REFERENCES resource (id)
            ON DELETE CASCADE;