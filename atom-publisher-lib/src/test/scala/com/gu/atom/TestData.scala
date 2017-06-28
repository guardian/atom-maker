package com.gu.atom

import java.util.Date

import com.gu.contentatom.thrift.AtomType.Profile
import com.gu.contentatom.thrift.atom.cta.CTAAtom
import com.gu.contentatom.thrift.atom.media._
import com.gu.contentatom.thrift.atom.profile.{ProfileAtom, ProfileItem}
import com.gu.contentatom.thrift.{ContentAtomEvent, _}

object TestData {
  val testAtoms = List(
    Atom(
      id = "1",
      atomType = AtomType.Media,
      defaultHtml = "<div></div>",
      data = AtomData.Media(
        MediaAtom(
          activeVersion = Some(2L),
          title = "Test atom 1",
          assets = List(
            Asset(
              assetType = AssetType.Video,
              version = 1L,
              id = "xyzzy",
              platform = Platform.Youtube
            ),
            Asset(
              assetType = AssetType.Video,
              version = 2L,
              id = "fizzbuzz",
              platform = Platform.Youtube
            )
          ),
          category = Category.News,
          plutoProjectId = None,
          duration = None,
          source = None,
          posterUrl = None,
          description = None,
          metadata = None
        )
      ),
      contentChangeDetails = ContentChangeDetails(revision = 1)
    ),
    Atom(
      id = "2",
      atomType = AtomType.Media,
      defaultHtml = "<div></div>",
      data = AtomData.Media(
        MediaAtom(
          activeVersion = None,
          title = "Test atom 2",
          assets = List(
            Asset(
              assetType = AssetType.Video,
              version = 1L,
              id = "jdklsajflka",
              platform = Platform.Youtube
            ),
            Asset(
              assetType = AssetType.Video,
              version = 2L,
              id = "afkdljlwe",
              platform = Platform.Youtube
            )
          ),
          category = Category.Feature,
          plutoProjectId = None,
          duration = None,
          source = None,
          posterUrl = None,
          description = None,
          metadata = None
        )
      ),
      contentChangeDetails = ContentChangeDetails(revision = 1)
    ),
    Atom(
      id = "3",
      atomType = AtomType.Cta,
      defaultHtml = "<div></div>",
      title = Some("The only CTA atom"),
      data = AtomData.Cta(CTAAtom(url = "http://lalala.com")),
      contentChangeDetails = ContentChangeDetails(revision = 1)
    )
  )

  def testAtomEvents = testAtoms.map(testAtomEvent)

  val testAtom = testAtoms.head

  val testAtomForDeletion = Atom(
    id = "delete",
    atomType = AtomType.Media,
    defaultHtml = "<div></div>",
    data = AtomData.Media(
      MediaAtom(
        activeVersion = Some(2L),
        title = "Test atom for deletion",
        assets = List(
          Asset(
            assetType = AssetType.Video,
            version = 1L,
            id = "123",
            platform = Platform.Youtube
          ),
          Asset(
            assetType = AssetType.Video,
            version = 2L,
            id = "456z",
            platform = Platform.Youtube
          )
        ),
        category = Category.News,
        plutoProjectId = None,
        duration = None,
        source = None,
        posterUrl = None,
        description = None,
        metadata = None
      )
    ),
    contentChangeDetails = ContentChangeDetails(revision = 1)
  )

  val profileAtom = Atom(
    id = "profile",
    atomType = AtomType.Profile,
    defaultHtml = "<div />",
    data = AtomData.Profile(
      ProfileAtom(
        items = List(
          ProfileItem(body = "test")
        )
      )
    ),
    contentChangeDetails = ContentChangeDetails(revision = 1)
  )

  def testAtomEvent(atom: Atom = testAtom) =
    ContentAtomEvent(testAtom, EventType.Update, new Date().getTime)
}
