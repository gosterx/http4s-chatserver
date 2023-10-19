package com.example.chat

import com.example.chat.domain.domain.UserName
import com.example.chat.domain.domain.RoomName
import com.example.chat.domain.OutputMessage
import com.example.chat.domain.InputMessage.*
import com.example.chat.domain.OutputMessage.*
import domain.InputMessage

final case class ChatState(
    usersRoom: Map[UserName, RoomName]
):
  def process(message: InputMessage): (ChatState, List[OutputMessage]) =
    message match
      case Help(user) =>
        (this, List(DirectMessage(user, domain.InputMessage.HelpText)))

      case ListRooms(user) =>
        val rooms = usersRoom.values.toList.distinct
        (this, List(DirectMessage(user, s"Rooms: ${rooms.mkString(",")}")))

      case ListMembers(user) =>
        val message = usersRoom.get(user).fold("You are not currently in a room") { room =>
          val members = roomMembers(room)
          s"Members: ${members.mkString(",")}"
        }
        (this, List(DirectMessage(user, message)))

      case Chat(user, text) =>
        usersRoom.get(user) match
          case Some(room) =>
            (this, List(RoomMessage(roomMembers(room), s"$user: $text")))
          case None =>
            (this, List(DirectMessage(user, "You are not currently in a room")))

      case EnterRoom(user, room) =>
        (ChatState(usersRoom + (user -> room)), List(DirectMessage(user, s"Joined into $room")))

      case Disconnect(user) =>
        (ChatState(usersRoom - user), List.empty)

      case InvalidInput(user, text) =>
        (this, List(DirectMessage(user, text)))

  private def roomMembers(room: RoomName): List[UserName] =
    usersRoom.collect {
      case (userName, roomName) if roomName == room => userName
    }.toList.distinct

object ChatState:
  def empty: ChatState = ChatState(Map.empty)
