package com.jspark.pw3_attendant.controller;

import com.jspark.pw3_attendant.service.inoutday.NewFriendService;
import com.jspark.pw3_attendant.service.inoutday.dto.NewFriendRequest;
import com.jspark.pw3_attendant.service.inoutday.dto.NewFriendResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/new-friends")
public class NewFriendController {

    private final NewFriendService newFriendService;

    @PostMapping
    public ResponseEntity<NewFriendResponse> saveNewFriend(@RequestBody NewFriendRequest request) {
        return ResponseEntity.ok(newFriendService.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewFriendResponse> updateNewFriend(@PathVariable Long id, @RequestBody NewFriendRequest request) {
        return ResponseEntity.ok(newFriendService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNewFriend(@PathVariable Long id) {
        newFriendService.delete(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<NewFriendResponse>> findAllNewFriends() {
        return ResponseEntity.ok(newFriendService.findAll());
    }
}
