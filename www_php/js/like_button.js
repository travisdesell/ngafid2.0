'use strict';

class LikeButton extends React.Component {
    constructor(props) {
        super(props);
        this.state = { liked: false };
    }

    render() {
        if (this.state.liked) {
            return 'You liked this.';
        }

        return (
            <button onClick={() => navbar.toggleLoggedIn()}>
                Like
            </button>
        );

        /*
        return (
            <button onClick={() => this.setState({ liked: true })}>
                Like
            </button>
        );
        */
    }
}


//const domContainer = document.querySelector('#like_button_container');
var likeButton = ReactDOM.render(
    <LikeButton />,
    document.querySelector('#like_button_container')
);

