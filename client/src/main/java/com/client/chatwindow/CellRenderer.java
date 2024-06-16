package com.client.chatwindow;

import com.messages.User;
import com.messages.Conversation;

import javafx.geometry.Pos;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;

/*
 * A Class for Rendering users images / name on the userlist.

class CellRenderer implements Callback<ListView<User>,ListCell<User>>{
        @Override
    public ListCell<User> call(ListView<User> p) {

        ListCell<User> cell = new ListCell<User>(){

            @Override
            protected void updateItem(User user, boolean bln) {
                super.updateItem(user, bln);
                setGraphic(null);
                setText(null);
                if (user != null) {
                    HBox hBox = new HBox();

                    Text name = new Text(user.getName());

                    ImageView statusImageView = new ImageView();
                    Image statusImage = new Image(getClass().getClassLoader().getResource("images/" + user.getStatus().toString().toLowerCase() + ".png").toString(), 16, 16,true,true);
                    statusImageView.setImage(statusImage);

                    ImageView pictureImageView = new ImageView();
                    Image image = new Image(getClass().getClassLoader().getResource("images/default.png").toString(),50,50,true,true);
                    pictureImageView.setImage(image);

                    hBox.getChildren().addAll(statusImageView, pictureImageView, name);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(hBox);
                }
            }
        };
        return cell;
    }
}
*/
/*
 * A Class for Rendering conversation images / name on the userlist.
 */
class CellRenderer implements Callback<ListView<Conversation>,ListCell<Conversation>>{
	@Override
    public ListCell<Conversation> call(ListView<Conversation> p) {

        ListCell<Conversation> cell = new ListCell<Conversation>(){

            @Override
            protected void updateItem(Conversation conversation, boolean bln) {
                super.updateItem(conversation, bln);
                setGraphic(null);
                setText(null);
                if (conversation != null) {
                    HBox hBox = new HBox();

                    Text name = new Text(conversation.getConversationName());
                    
					ImageView statusImageView = new ImageView();
                    Image statusImage = new Image(getClass().getClassLoader().getResource("images/" + conversation.getUserStatus().name() + ".png").toString(), 16, 16,true,true);
                    statusImageView.setImage(statusImage);

                    ImageView pictureImageView = new ImageView();
                    Image image = new Image(getClass().getClassLoader().getResource("images/default.png").toString(),50,50,true,true);
                    pictureImageView.setImage(image);

                    hBox.getChildren().addAll(statusImageView, pictureImageView, name);
                    hBox.setAlignment(Pos.CENTER_LEFT);

                    setGraphic(hBox);
                }
            }
        };
        return cell;
    }
}
