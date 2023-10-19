package com.example.chat.domain

object domain:
  opaque type UserName = String

  object UserName:
    def apply(user: String): UserName = user

  opaque type RoomName = String

  object RoomName:
    def apply(room: String): RoomName = room