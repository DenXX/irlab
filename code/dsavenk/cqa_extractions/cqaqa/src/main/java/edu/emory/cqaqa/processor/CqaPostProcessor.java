package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.CqaPost;

/**
 * Is used by parser for a callback.
 */
public interface CqaPostProcessor {
    /**
     * Process a CqaPost and returns it back.
     * @param post A post to process.
     * @return Returns processed CqaPost or null if the post should be filtered.
     */
    public CqaPost processPost(CqaPost post);
}
