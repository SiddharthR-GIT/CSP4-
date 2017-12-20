// Fig. 18.8: TicTacToeServer.java
// This class maintains a game of Tic-Tac-Toe for two client applets.
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import javax.swing.*;

public class TicTacToeServer extends JFrame {
   private char[] board;
   private JTextArea outputArea;
   private Player[] players;
   private ServerSocket server;
   private int count = 0;
   private int currentPlayer;
   private final int PLAYER_X = 0, PLAYER_O = 1;
   private final char X_MARK = 'X', O_MARK = 'O';
   private final char emptyBox = ' ';

   // set up tic-tac-toe server and GUI that displays messages
   public TicTacToeServer(int port)
   {
      super( "Tic-Tac-Toe Server" );

      board = new char[ 9 ];
      players = new Player[ 2 ];
      currentPlayer = PLAYER_X;

      // set up ServerSocket
      try {
         server = new ServerSocket( 12345, 2 );
      }

      // process problems creating ServerSocket
      catch( IOException ioException ) {
         ioException.printStackTrace();
         System.exit( 1 );
      }

      // set up JTextArea to display messages during execution
      outputArea = new JTextArea();
      getContentPane().add( outputArea, BorderLayout.CENTER );
      outputArea.setText( "Server awaiting connections\n" );

      setSize( 300, 300 );
      setVisible( true );

   } // end TicTacToeServer constructor

   // wait for two connections so game can be played
   public void execute()
   {
      // wait for each client to connect
      for ( int i = 0; i < players.length; i++ ) {

         // wait for connection, create Player, start thread
         try {
            players[ i ] = new Player( server.accept(), i );
            players[ i ].start();
         }

         // process problems receiving connection from client
         catch( IOException ioException ) {
            ioException.printStackTrace();
            System.exit( 1 );
         }
      }

      // Player X is suspended until Player O connects.
      // Resume player X now.
      synchronized ( players[ PLAYER_X ] ) {
         players[ PLAYER_X ].setSuspended( false );
         players[ PLAYER_X ].notify();
      }

   }  // end method execute

   // utility method called from other threads to manipulate
   // outputArea in the event-dispatch thread
   private void displayMessage( final String messageToDisplay )
   {
      // display message from event-dispatch thread of execution
      SwingUtilities.invokeLater(
              new Runnable() {  // inner class to ensure GUI updates properly

                 public void run() // updates outputArea
                 {
                    outputArea.append( messageToDisplay );
                    outputArea.setCaretPosition(
                            outputArea.getText().length() );
                 }

              }  // end inner class

      ); // end call to SwingUtilities.invokeLater
   }

   // Determine if a move is valid. This method is synchronized because
   // only one move can be made at a time.
   public synchronized boolean validateAndMove( int location, int player )
   {
      boolean moveDone = false;
//      if(isGameOver() ==true){
//         for (int i = 0; i <board.length ; i++) {
//            board[ i ] = emptyBox;
//         }
//      }
      // while not current player, must wait for turn
      while ( player != currentPlayer ) {

         // wait for turn
         try {
            wait();
         }

         // catch wait interruptions
         catch( InterruptedException interruptedException ) {
            interruptedException.printStackTrace();
         }
      }

      // if location not occupied, make move
      if ( !isOccupied( location ) ) {

         // set move in board array
         board[ location ] = currentPlayer == PLAYER_X ? X_MARK : O_MARK;

         // change current player
         currentPlayer = ( currentPlayer + 1 ) % 2;

         // let new current player know that move occurred
         players[ currentPlayer ].otherPlayerMoved( location );

         notify(); // tell waiting player to continue

         // tell player that made move that the move was valid
         return true;
      }

      // tell player that made move that the move was not valid
      else
         return false;

   } // end method validateAndMove

   // determine whether location is occupied
   public boolean isOccupied( int location )
   {
      if ( board[ location ] == X_MARK || board [ location ] == O_MARK )
         return true;
      else

         return false;
   }

   public boolean isGameOver()
   {
      if ((board[0] == X_MARK && board[0] == board[1] && board[0] == board[2])
              || (board[3] == X_MARK && board[3] ==board[4] && board[3] == board[5])
              || (board[6]== X_MARK && board[6] == board[7] && board[6] == board[8])
              || (board[0]== X_MARK && board[0] == board[3] && board[0] == board[6])
              || (board[1]== X_MARK && board[1] == board[4] && board[1] == board[7])
              || (board[2]== X_MARK && board[2] == board[5] && board[2] == board[8])
              || (board[0]== X_MARK && board[0] ==board[4] && board[0] == board[8])
              || (board[2]== X_MARK && board[2] == board[4] && board[2] == board[6]))
      {
         players[PLAYER_X].winner(1);
         players[PLAYER_O].winner(1);
         return true;
      }
      else if ((board[0] == O_MARK && board[0] == board[1] && board[0] == board[2])
              || (board[3] == O_MARK && board[3] ==board[4] && board[3] == board[5])
              || (board[6]== O_MARK && board[6] == board[7] && board[6] == board[8])
              || (board[0]== O_MARK && board[0] == board[3] && board[0] == board[6])
              || (board[1]== O_MARK && board[1] == board[4] && board[1] == board[7])
              || (board[2]== O_MARK && board[2] == board[5] && board[2] == board[8])
              || (board[0]== O_MARK && board[0] ==board[4] && board[0] == board[8])
              || (board[2]== O_MARK && board[2] == board[4] && board[2] == board[6]))
      {
         players[PLAYER_X].winner(2);
         players[PLAYER_O].winner(2);
         return true;
      }
      else if (count == 9)
      {
         players[PLAYER_X].winner(3);
         players[PLAYER_O].winner(3);
         return true;
      }
      return false;
   }

   public static void main( String args[] )
   {
      int port =12345;
      try{
         port = Integer.parseInt(args[0]);
      }catch(NumberFormatException|ArrayIndexOutOfBoundsException e) {}
      TicTacToeServer application = new TicTacToeServer(port);
      application.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
      application.execute();
   }

   // private inner class Player manages each Player as a thread
   private class Player extends Thread {
      private Socket connection;
      private DataInputStream input;
      private DataOutputStream output;
      private int playerNumber;
      private char mark;
      protected boolean suspended = true;

      // set up Player thread
      public Player( Socket socket, int number )
      {
         playerNumber = number;

         // specify player's mark
         mark = ( playerNumber == PLAYER_X ? X_MARK : O_MARK );

         connection = socket;

         // obtain streams from Socket
         try {
            input = new DataInputStream( connection.getInputStream() );
            output = new DataOutputStream( connection.getOutputStream() );
         }

         // process problems getting streams
         catch( IOException ioException ) {
            ioException.printStackTrace();
            System.exit( 1 );
         }


      } // end Player constructor
      public  void  winner( int num){
         if (num == 1){
            try {
               output.writeUTF( "player X winner \n" );
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
         else if (num == 2){
            try {
               output.writeUTF( "player O winner \n" );
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
         else if (num == 3){
            try {
               output.writeUTF( "Draw \n" );
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

      // send message that other player moved
      public void otherPlayerMoved( int location )
      {
         // send message indicating move
         try {
            count ++;
            output.writeUTF( "Opponent moved" );
            output.writeInt( location );
         }

         // process problems sending message
         catch ( IOException ioException ) {
            ioException.printStackTrace();
         }
      }

      // control thread's execution
      public void run()
      {
         // send client message indicating its mark (X or O),
         // process messages from client
         try {
            displayMessage( "Player " + ( playerNumber ==
                    PLAYER_X ? X_MARK : O_MARK ) + " connected\n" );

            output.writeChar( mark ); // send player's mark

            // send message indicating connection
            output.writeUTF( "Player " + ( playerNumber == PLAYER_X ?
                    "X connected\n" : "O connected, please wait\n" ) );

            // if player X, wait for another player to arrive
            if ( mark == X_MARK ) {
               output.writeUTF( "Waiting for another player" );

               // wait for player O
               try {
                  synchronized( this ) {
                     while ( suspended )
                        wait();
                  }
               }

               // process interruptions while waiting
               catch ( InterruptedException exception ) {
                  exception.printStackTrace();
               }

               // send message that other player connected and
               // player X can make a move
               output.writeUTF( "Other player connected. Your move." );
            }

            // while game not over
            while ( ! isGameOver() ) {

               // get move location from client
               int location = input.readInt();

               // check for valid move
               if ( validateAndMove( location, playerNumber ) ) {
                  displayMessage( "\nlocation: " + location );
                  output.writeUTF( "Valid move." );
               }
               else
                  output.writeUTF( "Invalid move, try again" );
            }
/*
            if(isGameOver() == true){
                  count = 0;
               System.out.println("Game");
               int location = input.readInt();
               if(location == 10) {
                  TicTacToeServer.this.execute();
               }
            }*/
            connection.close(); // close connection to client
         } // end try

         // process problems communicating with client
         catch( IOException ioException ) {
            ioException.printStackTrace();
            System.exit( 1 );
         }

      } // end method run

      // set whether or not thread is suspended
      public void setSuspended( boolean status )
      {
         suspended = status;
      }

   } // end class Player

} // end class TicTacToeServer

/**************************************************************************
 * (C) Copyright 1992-2003 by Deitel & Associates, Inc. and               *
 * Prentice Hall. All Rights Reserved.                                    *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
