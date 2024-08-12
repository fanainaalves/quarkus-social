package io.github.fanainaalves.quarkussocial.rest;

import io.github.fanainaalves.quarkussocial.domain.model.Post;
import io.github.fanainaalves.quarkussocial.domain.model.User;
import io.github.fanainaalves.quarkussocial.domain.repository.FollowerRepository;
import io.github.fanainaalves.quarkussocial.domain.repository.PostRepository;
import io.github.fanainaalves.quarkussocial.domain.repository.UserRepository;
import io.github.fanainaalves.quarkussocial.rest.dto.CreatePostRequest;
import io.github.fanainaalves.quarkussocial.rest.dto.PostResponse;
import io.quarkus.panache.common.Sort;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.stream.Collectors;

@Path("/users/{userId}/posts")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PostResource {

    private UserRepository userRepository;
    private PostRepository postRepository;
    private FollowerRepository followerRepository;

    @Inject
    public PostResource(UserRepository userRepository, PostRepository postRepository, FollowerRepository followerRepository){
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.followerRepository = followerRepository;
    }

    @POST
    @Transactional
    public Response savePost(@PathParam("userId") Long userId, CreatePostRequest request){
        User user = userRepository.findById(userId);
        if(user == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        Post post = new Post();
        post.setText(request.getText());
        post.setUser(user);

        postRepository.persist(post);
        return Response.status(Response.Status.CREATED).build();
    }

    @GET
    public Response listPosts(@PathParam("userId") Long userId, @HeaderParam("followerId") Long followerId){
        User user = userRepository.findById(userId);
        if(user == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if(followerId == null){
            return Response.status(Response.Status.BAD_REQUEST).entity("You forgot the header followerId").build();
        }

        User follower = userRepository.findById(followerId);

        if(follower == null){
            return Response.status(Response.Status.BAD_REQUEST).entity("Inexistent followerId").build();
        }

        boolean follows = followerRepository.follows(follower, user);
        if(!follows){
            return Response.status(Response.Status.FORBIDDEN).entity("You can't see these posts").build();
        }

        var query = postRepository.find( "user", Sort.by("dateTime", Sort.Direction.Descending), user);
        var list = query.list();
        var postResponseList = list.stream().map(PostResponse::fromEntity).collect(Collectors.toList());

        return Response.ok(postResponseList).build();
    }


}
