package club.hellin.searcher;

import club.hellin.API;
import club.hellin.objects.impl.Post;
import club.hellin.objects.impl.RedditAccessToken;
import club.hellin.objects.impl.RedditDetails;
import club.hellin.utils.Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class Main {
    public static final Scanner SCANNER = new Scanner(System.in);
    public static final File REDDIT_DETAILS_FILE = new File("./details.json");
    public static final File REDDIT_ACCESS_TOKEN_FILE = new File("./access_token.json");
    public static final File USER_POSTS_DIR = new File("./users");

    public static void main(final String[] args) throws IOException {
        RedditDetails details = getRedditDetails();

        if (details == null) {
            final RedditDetails.RedditDetailsBuilder builder = RedditDetails.builder();

            System.out.println("Username: ");
            builder.username(SCANNER.nextLine());

            System.out.println("Password: ");
            builder.password(SCANNER.nextLine());

            System.out.println("ID: ");
            builder.id(SCANNER.nextLine());

            System.out.println("Secret: ");
            builder.secret(SCANNER.nextLine());

            details = builder.build();
        }

        details.save(REDDIT_DETAILS_FILE);

        RedditAccessToken token = getAccessToken();

        if (token == null || token.isExpired())
            token = API.getRedditAccessToken(details);

        token.save(REDDIT_ACCESS_TOKEN_FILE);

        System.out.println("Enter the username to search for: ");
        final String username = SCANNER.nextLine();

        try {
            final List<Post> posts = API.retrievePosts(username, token);
            final List<JSONObject> postJsonObjects = new ArrayList<>();

            for (final Post post : posts)
                postJsonObjects.add(post.toJson());

            save(postJsonObjects, username);
        } catch (final IOException exception) {
            exception.printStackTrace();

            // The access token might be not working
            // Lets just delete it just in case
            REDDIT_ACCESS_TOKEN_FILE.delete();
        }

        SCANNER.close();
    }

    private static void save(final List<JSONObject> posts, final String username) throws IOException {
        final File file = new File(USER_POSTS_DIR, String.format("%s.json", username));
        final File parent = file.getParentFile();

        if (!parent.exists())
            parent.mkdirs();

        if (!file.exists())
            file.createNewFile();

        System.out.println(String.format("Saving %s posts from user %s.", posts.size(), username));

        final JSONObject obj = new JSONObject();
        obj.put("posts", new JSONArray(posts));

        final String json = Utils.jsonToStr(obj, true);
        final FileWriter writer = new FileWriter(file);

        writer.write(json);
        writer.close();
    }

    private static RedditDetails getRedditDetails() throws IOException {
        if (!REDDIT_DETAILS_FILE.exists())
            return null;

        return new RedditDetails(new String(Files.readAllBytes(REDDIT_DETAILS_FILE.toPath())));
    }

    private static RedditAccessToken getAccessToken() throws IOException {
        if (!REDDIT_ACCESS_TOKEN_FILE.exists())
            return null;

        return new RedditAccessToken(new String(Files.readAllBytes(REDDIT_ACCESS_TOKEN_FILE.toPath())));
    }
}