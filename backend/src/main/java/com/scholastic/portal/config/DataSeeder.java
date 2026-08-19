package com.scholastic.portal.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.scholastic.portal.model.Book;
import com.scholastic.portal.model.Role;
import com.scholastic.portal.model.User;
import com.scholastic.portal.repository.BookRepository;
import com.scholastic.portal.repository.UserRepository;

/**
 * Seeds a small demo dataset (idempotent) so the app is usable immediately after boot.
 * In a real product this would be a proper bootstrap/migration; here it keeps local + cloud
 * provisioning uniform for the demo.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      BookRepository bookRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(new User("teacher", passwordEncoder.encode("password"), "Ms. Rivera", Role.TEACHER));
        userRepository.save(new User("student1", passwordEncoder.encode("password"), "Ava", Role.STUDENT));
        userRepository.save(new User("student2", passwordEncoder.encode("password"), "Liam", Role.STUDENT));
        userRepository.save(new User("student3", passwordEncoder.encode("password"), "Maya", Role.STUDENT));
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) {
            return;
        }
        bookRepository.saveAll(List.of(
                book("The Great Gatsby",
                     "F. Scott Fitzgerald",
                     "A modern tale of ambition and heartbreak in the Jazz Age.",
                     "In my younger and more vulnerable years my father gave me some advice that I've been " +
                     "turning over in my mind ever since.\n\n'Whenever you feel like criticizing any one,' he told me, " +
                     "'just remember that all the people in this world haven't had the advantages that you've had.'\n\n" +
                     "He didn't say any more, but we have always been especially warm in a complementary way about my " +
                     "dad's advice. When he came back from the war, he went directly to work in the bond business, and " +
                     "became very successful. Living in West Egg, he found himself across the bay from the mysterious " +
                     "Jay Gatsby, a neighbor whose weekend parties became the talk of the town.",
                     "https://www.gutenberg.org/ebooks/64317"),
                book("The Little Prince",
                     "Antoine de Saint-Exupéry",
                     "A gentle philosophical fable about imagination, friendship, and what really matters.",
                     "Once upon a time there was a little prince who lived on a small planet. It had to be kept clean " +
                     "of baobabs, and the little prince tended his rose with great care each morning. 'It is only with " +
                     "the heart that one can see rightly,' the fox said to the little prince. 'What is essential is " +
                     "invisible to the eye.' Each time he travels, he carries with him the simple power of wonder — " +
                     "and the reminder that one must never forget what one taught.",
                     "https://www.gutenberg.org/ebooks/345"),
                book("A Tale of Two Cities",
                     "Charles Dickens",
                     "It was the best of times, it was the worst of times.",
                     "It was the best of times, it was the worst of times, it was the age of wisdom, it was the age " +
                     "of foolishness, it was the epoch of belief, it was the epoch of incredulity, it was the season " +
                     "of Light, it was the season of Darkness, it was the spring of hope, it was the winter of " +
                     "despair, we had everything before us, we had nothing before us. In all such times the heart " +
                     "of the people must be saved by that which is best in it — and the story that follows opens " +
                     "on the turn of a tide.",
                     "https://www.gutenberg.org/ebooks/98")));
    }

    private Book book(String title, String author, String description, String content, String ref) {
        return new Book(title, author, description, content, ref);
    }
}