package com.codinglemonsbackend.Entities;

import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Every problem a user has liked, as one set on one document keyed by username.
 *
 * The usual objection to arrays inside documents is unbounded growth, which does not apply here:
 * this array is bounded by the size of the problem catalogue, not by how often the user clicks.
 * In exchange, $addToSet provides natively the duplicate guarantee that a unique compound index
 * used to enforce, the document count drops from one per like to one per user, and "which problems
 * has this user liked" - the one question the row-per-like shape had no index for at all - becomes
 * a single read by _id.
 *
 * The per-like timestamp is deliberately gone. Nothing ever read it, and keeping it here would
 * mean an array of subdocuments, which costs exactly the simplicity this shape exists for.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "UserLikedProblems")
public class UserLikedProblems {

    @Id
    private String username;

    private Set<Integer> likedProblemIds;
}
