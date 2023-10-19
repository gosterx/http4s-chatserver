package com.example.chat.domain

import com.example.chat.domain.domain.UserName
import com.example.chat.domain.domain.RoomName

sealed trait InputMessage

object InputMessage:
  final val DefaultRoomName = RoomName("default")

  final val HelpText: String =
    """Commands:
      |  /help             - Show this text
      |  /room             - Change to default/entry room
      |  /room <room name> - Change to specified room
      |  /rooms            - List all rooms
      |  /members          - List members in current room
    """.stripMargin

  case class Help(user: UserName)                                        extends InputMessage
  case class Chat(user: UserName, text: String)                          extends InputMessage
  case class EnterRoom(user: UserName, room: RoomName = DefaultRoomName) extends InputMessage
  case class ListRooms(user: UserName)                                   extends InputMessage
  case class ListMembers(user: UserName)                                 extends InputMessage
  case class Disconnect(user: UserName)                                  extends InputMessage
  case class InvalidInput(user: UserName, text: String)                  extends InputMessage

  def parse(user: UserName, text: String): InputMessage =
    text.split(" ").toList match
      case "/help" :: Nil                  => Help(user)
      case "/room" :: Nil                  => EnterRoom(user)
      case "/room" :: room :: Nil          => EnterRoom(user, RoomName(room.toLowerCase))
      case "/rooms" :: Nil                 => ListRooms(user)
      case "/members" :: Nil               => ListMembers(user)
      case command if text.startsWith("/") => InvalidInput(user, s"Unknown command - $text")
      case _                               => Chat(user, text)
