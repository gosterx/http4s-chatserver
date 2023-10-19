package com.example.chat.domain

import com.example.chat.domain.domain.UserName

trait OutputMessage:
  def directedTo(user: UserName): Boolean
  def textToSend: String

object OutputMessage:
  case class WelcomeMessage(user: UserName) extends OutputMessage:
    override def directedTo(user: UserName): Boolean = user == this.user
    override def textToSend: String                  = s"Welcome to the Chat Server, $user"

  case class DirectMessage(user: UserName, text: String) extends OutputMessage:
    override def directedTo(user: UserName): Boolean = user == this.user
    override def textToSend: String                  = this.text

  case class RoomMessage(roomUsers: List[UserName], text: String) extends OutputMessage:
    override def directedTo(user: UserName): Boolean = roomUsers.contains(user)
    override def textToSend: String                  = text

  case object KeepAlive extends OutputMessage:
    override def directedTo(user: UserName): Boolean = true
    override def textToSend: String                  = ""
