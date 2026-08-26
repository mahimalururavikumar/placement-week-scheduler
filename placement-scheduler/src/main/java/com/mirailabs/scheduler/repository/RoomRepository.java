package com.mirailabs.scheduler.repository;

import com.mirailabs.scheduler.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomCode(String roomCode);

    List<Room> findByActiveTrue();

    long countByActiveTrue();
}