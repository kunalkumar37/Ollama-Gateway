package com.agentcore.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.agentcore.gateway.entity.ApiKeyEntity;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKeyEntity, Long>{
    Optional<ApiKeyEntity> findByKeyValueAndActiveTrue(String keyValue);
    Optional<ApiKeyEntity> findByKeyValue(String keyValue);

    @Modifying
        @Query("UPDATE ApiKeyEntity k SET k.totalRequests = k.totalRequests + 1," +
           " k.lastUsedAt = :now WHERE k.keyValue = :keyValue")

    void incrementRequests(String keyValue, Instant now);
    

}
