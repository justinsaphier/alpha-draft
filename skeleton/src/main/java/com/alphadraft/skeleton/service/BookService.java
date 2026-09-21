package com.alphadraft.skeleton.service;

import com.alphadraft.skeleton.model.Book;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final List<Book> books = List.of(
            new Book(1, "The Great Gatsby", "F. Scott Fitzgerald"),
            new Book(2, "1984", "George Orwell"),
            new Book(3, "To Kill a Mockingbird", "Harper Lee"));

    public List<Book> getAllBooks() {
        return books;
    }
}
