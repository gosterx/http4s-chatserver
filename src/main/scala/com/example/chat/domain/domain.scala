package com.example.chat.domain

object domain:
  opaque type UserName = String

  object UserName:
    def apply(user: String): UserName          = user
    def unapply(str: String): Option[UserName] = Some(UserName(str))

  opaque type RoomName = String

  object RoomName:
    def apply(room: String): RoomName = room
