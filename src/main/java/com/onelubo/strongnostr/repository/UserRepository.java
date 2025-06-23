package com.onelubo.strongnostr.repository;

import com.onelubo.strongnostr.model.user.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByNostrPubKey(String nostrPublicKey);

    boolean existsByNostrPublicKey(String nostrPublicKey);

    boolean existsByUsername(String username);
}
